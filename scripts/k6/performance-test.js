import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginTrend = new Trend('login_duration');
const apiTrend = new Trend('api_duration');

// Test configuration
export let options = {
  scenarios: {
    // Smoke test
    smoke_test: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      tags: { test_type: 'smoke' },
    },

    // Load test
    load_test: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m', target: 10 },   // Ramp up to 10 users over 1 minute
        { duration: '3m', target: 10 },   // Stay at 10 users for 3 minutes
        { duration: '1m', target: 50 },   // Ramp up to 50 users over 1 minute
        { duration: '5m', target: 50 },   // Stay at 50 users for 5 minutes
        { duration: '1m', target: 100 },  // Ramp up to 100 users over 1 minute
        { duration: '5m', target: 100 },  // Stay at 100 users for 5 minutes
        { duration: '1m', target: 0 },    // Ramp down to 0 users over 1 minute
      ],
      tags: { test_type: 'load' },
    },

    // Stress test
    stress_test: {
      executor: 'ramping-vus',
      stages: [
        { duration: '2m', target: 100 },  // Ramp up to 100 users
        { duration: '5m', target: 100 },  // Stay at 100 users
        { duration: '2m', target: 200 },  // Ramp up to 200 users
        { duration: '5m', target: 200 },  // Stay at 200 users
        { duration: '2m', target: 300 },  // Ramp up to 300 users
        { duration: '5m', target: 300 },  // Stay at 300 users
        { duration: '2m', target: 0 },    // Ramp down
      ],
      tags: { test_type: 'stress' },
    },
  },

  thresholds: {
    // HTTP request duration should be < 500ms for 95% of requests
    http_req_duration: ['p(95)<500'],

    // Error rate should be < 1%
    errors: ['rate<0.01'],

    // 95% of requests should be below 300ms
    api_duration: ['p(95)<300'],
  },
};

// Base URL
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Global variables
let authToken = '';

// Setup function - runs before the test starts
export function setup() {
  console.log('Starting performance test setup...');

  // Login to get authentication token
  const loginResponse = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({
      username: 'admin',
      password: 'admin123'
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  check(loginResponse, {
    'login successful': (r) => r.status === 200,
    'login has token': (r) => r.json('data.token') !== undefined,
  });

  if (loginResponse.status === 200) {
    authToken = loginResponse.json('data.token');
    console.log('Authentication successful');
  } else {
    console.error('Authentication failed:', loginResponse.body);
  }

  return { authToken };
}

// Default function - the VU code runs here
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${authToken || data.authToken}`,
  };

  // Test 1: Health check
  const healthStart = new Date().getTime();
  const healthResponse = http.get(`${BASE_URL}/actuator/health`);
  const healthDuration = new Date().getTime() - healthStart;

  const healthCheck = check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
    'health response time < 200ms': (r) => r.timings.duration < 200,
  });

  errorRate.add(!healthCheck);
  apiTrend.add(healthDuration);

  // Test 2: Get current user info
  const userStart = new Date().getTime();
  const userResponse = http.get(`${BASE_URL}/api/v1/auth/me`, { headers });
  const userDuration = new Date().getTime() - userStart;

  const userCheck = check(userResponse, {
    'user info status is 200': (r) => r.status === 200,
    'user info has username': (r) => r.json('data.username') !== undefined,
  });

  errorRate.add(!userCheck);
  apiTrend.add(userDuration);

  // Test 3: Get robots list
  const robotsStart = new Date().getTime();
  const robotsResponse = http.get(`${BASE_URL}/api/v1/robots`, { headers });
  const robotsDuration = new Date().getTime() - robotsStart;

  const robotsCheck = check(robotsResponse, {
    'robots status is 200': (r) => r.status === 200,
    'robots response is array': (r) => Array.isArray(r.json('data')),
  });

  errorRate.add(!robotsCheck);
  apiTrend.add(robotsDuration);

  // Test 4: Get alarms list (if we have robots)
  if (robotsResponse.status === 200 && robotsResponse.json('data').length > 0) {
    const robotId = robotsResponse.json('data.0.id');

    const alarmsStart = new Date().getTime();
    const alarmsResponse = http.get(
      `${BASE_URL}/api/v1/alarms?page=0&size=10&robotId=${robotId}`,
      { headers }
    );
    const alarmsDuration = new Date().getTime() - alarmsStart;

    const alarmsCheck = check(alarmsResponse, {
      'alarms status is 200': (r) => r.status === 200,
      'alarms response has content': (r) => r.json('data.content') !== undefined,
    });

    errorRate.add(!alarmsCheck);
    apiTrend.add(alarmsDuration);
  }

  // Test 5: Get telemetry data (if we have robots)
  if (robotsResponse.status === 200 && robotsResponse.json('data').length > 0) {
    const robotId = robotsResponse.json('data.0.id');

    const telemetryStart = new Date().getTime();
    const telemetryResponse = http.get(
      `${BASE_URL}/api/v1/robots/${robotId}/latest`,
      { headers }
    );
    const telemetryDuration = new Date().getTime() - telemetryStart;

    const telemetryCheck = check(telemetryResponse, {
      'telemetry status is 200': (r) => r.status === 200,
      'telemetry has joint samples': (r) => r.json('data.jointSamples') !== undefined,
    });

    errorRate.add(!telemetryCheck);
    apiTrend.add(telemetryDuration);
  }

  // Random sleep between 1-3 seconds to simulate real user behavior
  sleep(Math.random() * 2 + 1);
}

// Handle summary - runs after the test completes
export function handleSummary(data) {
  const summary = {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'performance-report.json': JSON.stringify(data, null, 2),
  };

  // Generate HTML report if possible
  try {
    summary['performance-report.html'] = generateHTMLReport(data);
  } catch (error) {
    console.error('Failed to generate HTML report:', error);
  }

  return summary;
}

function textSummary(data, options) {
  return `
📊 Performance Test Summary
==========================

Test Duration: ${data.metrics.iteration_duration.values.avg}ms avg iteration
Total Requests: ${data.metrics.http_reqs.values.count}
Failed Requests: ${data.metrics.http_req_failed.values.rate * 100}%

Response Times:
- Average: ${Math.round(data.metrics.http_req_duration.values.avg)}ms
- 95th percentile: ${Math.round(data.metrics.http_req_duration.values['p(95)']}ms
- 99th percentile: ${Math.round(data.metrics.http_req_duration.values['p(99)']}ms

HTTP Status:
- 2xx: ${data.metrics.http_req_duration.values.count - data.metrics.http_req_failed.values.count}
- Errors: ${data.metrics.http_req_failed.values.count}

Custom Metrics:
- API Duration (avg): ${Math.round(data.metrics.api_duration.values.avg)}ms
- Error Rate: ${(data.metrics.errors.values.rate * 100).toFixed(2)}%

Test completed at: ${new Date().toISOString()}
`;
}

function generateHTMLReport(data) {
  return `
<!DOCTYPE html>
<html>
<head>
    <title>Industrial Robot OPS Performance Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .metric { background: #f5f5f5; padding: 20px; margin: 10px 0; border-radius: 5px; }
        .metric h3 { margin-top: 0; color: #333; }
        .value { font-size: 24px; font-weight: bold; color: #007acc; }
        .chart { margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <h1>🚀 Industrial Robot Operation Simulation Platform</h1>
    <h2>Performance Test Report</h2>

    <div class="metric">
        <h3>📈 Response Times</h3>
        <div class="value">${Math.round(data.metrics.http_req_duration.values.avg)}ms</div>
        <p>Average response time</p>
    </div>

    <div class="metric">
        <h3>✅ Success Rate</h3>
        <div class="value">${((1 - data.metrics.http_req_failed.values.rate) * 100).toFixed(1)}%</div>
        <p>Request success rate</p>
    </div>

    <div class="metric">
        <h3>🔥 Throughput</h3>
        <div class="value">${Math.round(data.metrics.http_reqs.values.rate)} req/s</div>
        <p>Requests per second</p>
    </div>

    <h3>Detailed Metrics</h3>
    <table>
        <tr><th>Metric</th><th>Value</th><th>Description</th></tr>
        <tr><td>Total Requests</td><td>${data.metrics.http_reqs.values.count}</td><td>Total number of HTTP requests made</td></tr>
        <tr><td>Average Response Time</td><td>${Math.round(data.metrics.http_req_duration.values.avg)}ms</td><td>Average response time across all requests</td></tr>
        <tr><td>95th Percentile</td><td>${Math.round(data.metrics.http_req_duration.values['p(95)'])}ms</td><td>95% of requests completed within this time</td></tr>
        <tr><td>99th Percentile</td><td>${Math.round(data.metrics.http_req_duration.values['p(99)'])}ms</td><td>99% of requests completed within this time</td></tr>
        <tr><td>Error Rate</td><td>${(data.metrics.errors.values.rate * 100).toFixed(2)}%</td><td>Percentage of failed requests</td></tr>
    </table>

    <p><small>Report generated at: ${new Date().toISOString()}</small></p>
</body>
</html>
`;
}
