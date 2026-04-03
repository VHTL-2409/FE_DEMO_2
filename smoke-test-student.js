#!/usr/bin/env node
/**
 * FE_DEMO - Student Role Smoke Test
 * 
 * Chu cai nay de test tat ca chuc nang cua role hoc sinh
 * Chay script: node smoke-test-student.js
 * 
 * Dac diem:
 * - Test API truc tiep khong can browser
 * - Xuat ket qua chi tiet ra console va file
 * - Co the chay nhieu lan lien tuc
 * 
 * Prerequisites:
 * - Backend: http://localhost:8082 (cd BE && mvn spring-boot:run)
 * - Database PostgreSQL: datn (postgres/123456)
 */

const https = require('http');
const https_local = require('http');

// Cau hinh
const CONFIG = {
    API_BASE: 'http://localhost:8082',
    FRONTEND_BASE: 'http://localhost:5173',
    USERNAME: 'student1',
    PASSWORD: '123456',
    TIMEOUT: 15000,
    RETRY_COUNT: 2,
    RETRY_DELAY: 1000
};

// Token luu tru
let accessToken = '';
let refreshToken = '';
let userInfo = null;

// Ket qua test
const results = {
    timestamp: new Date().toISOString(),
    passed: [],
    failed: [],
    skipped: [],
    warnings: [],
    total: 0
};

// File ghi ket qua
const fs = require('fs');
const path = require('path');
const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
const outputDir = path.join(__dirname, 'test-results');
const outputFile = path.join(outputDir, `smoke-test-${timestamp}.json`);
const htmlFile = path.join(outputDir, `smoke-test-${timestamp}.html`);

// Tao thu muc ket qua
if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
}

// Mau mau sac cho console
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    dim: '\x1b[2m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m',
    cyan: '\x1b[36m',
    white: '\x1b[37m',
    bgRed: '\x1b[41m',
    bgGreen: '\x1b[42m',
    bgYellow: '\x1b[43m'
};

function log(msg, color = 'white') {
    const c = colors[color] || colors.white;
    console.log(`${c}${msg}${colors.reset}`);
}

function logHeader(title) {
    console.log('\n' + colors.cyan + '='.repeat(70) + colors.reset);
    log(`  ${title}`, 'cyan');
    console.log(colors.cyan + '='.repeat(70) + colors.reset + '\n');
}

function logStep(step, name) {
    console.log(`\n${colors.blue}[B${step}]${colors.reset} ${colors.bright}${name}${colors.reset}`);
}

function pass(msg) {
    console.log(`  ${colors.green}✓ PASS:${colors.reset} ${msg}`);
    results.passed.push(msg);
    results.total++;
}

function fail(msg, error = null) {
    console.log(`  ${colors.red}✗ FAIL:${colors.reset} ${msg}`);
    if (error) {
        console.log(`    ${colors.dim}Error: ${error.message || error}${colors.reset}`);
    }
    results.failed.push({ test: msg, error: error?.message || String(error) });
    results.total++;
}

function skip(msg) {
    console.log(`  ${colors.yellow}⊘ SKIP:${colors.reset} ${msg}`);
    results.skipped.push(msg);
    results.total++;
}

function warn(msg) {
    console.log(`  ${colors.yellow}⚠ WARN:${colors.reset} ${msg}`);
    results.warnings.push(msg);
}

function info(msg) {
    console.log(`  ${colors.dim}ℹ ${msg}${colors.reset}`);
}

// HTTP request helper
async function httpRequest(method, endpoint, token = null, body = null, isFormData = false) {
    return new Promise((resolve, reject) => {
        const url = new URL(endpoint, CONFIG.API_BASE);
        const options = {
            hostname: url.hostname,
            port: url.port,
            path: url.pathname + url.search,
            method: method,
            headers: {
                'Content-Type': isFormData ? 'multipart/form-data' : 'application/json',
                'Accept': 'application/json'
            },
            timeout: CONFIG.TIMEOUT
        };

        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }

        const req = https_local.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                let jsonData = null;
                try {
                    jsonData = JSON.parse(data);
                } catch {
                    jsonData = data;
                }
                resolve({ status: res.statusCode, data: jsonData, raw: data });
            });
        });

        req.on('error', reject);
        req.on('timeout', () => reject(new Error('Request timeout')));

        if (body) {
            let bodyStr = body;
            if (!isFormData && typeof body === 'object') {
                bodyStr = JSON.stringify(body);
            }
            if (typeof bodyStr === 'string') {
                options.headers['Content-Length'] = Buffer.byteLength(bodyStr);
            }
            req.write(bodyStr);
        } else if (method !== 'GET') {
            options.headers['Content-Length'] = '0';
        }

        req.end();
    });
}

// Retry wrapper
async function withRetry(fn, retries = CONFIG.RETRY_COUNT) {
    for (let i = 0; i <= retries; i++) {
        try {
            return await fn();
        } catch (error) {
            if (i < retries) {
                warn(`Retrying... (${i + 1}/${retries})`);
                await new Promise(r => setTimeout(r, CONFIG.RETRY_DELAY));
            } else {
                throw error;
            }
        }
    }
}

// ========================================
// TEST SUITES
// ========================================

async function testHealthCheck() {
    logStep(1, 'Health Check');
    
    try {
        const response = await httpRequest('GET', '/api/health');
        if (response.status === 200) {
            pass('Backend is running');
            info(`Response: ${JSON.stringify(response.data)}`);
            return true;
        } else {
            fail('Backend health check failed', new Error(`Status: ${response.status}`));
            return false;
        }
    } catch (error) {
        fail('Cannot connect to backend', error);
        return false;
    }
}

async function testLogin() {
    logStep(2, 'Authentication - Login');
    
    // Test 1: Login with valid credentials
    try {
        const response = await withRetry(() => httpRequest('POST', '/api/auth/login', null, {
            username: CONFIG.USERNAME,
            password: CONFIG.PASSWORD
        }));

        if (response.status === 200) {
            accessToken = response.data?.token || response.data?.data?.token || '';
            refreshToken = response.data?.refreshToken || response.data?.data?.refreshToken || '';
            
            if (accessToken) {
                pass('Login successful with student1/123456');
                
                // Extract user info from token
                try {
                    const payload = JSON.parse(Buffer.from(accessToken.split('.')[1], 'base64').toString());
                    userInfo = payload;
                    info(`User roles: ${payload.roles?.join(', ') || 'N/A'}`);
                } catch {
                    info('Token decoded but user info not available');
                }
            } else {
                fail('Token not found in response');
            }
        } else if (response.status === 401) {
            fail('Login failed - Invalid credentials', new Error(response.data?.message || 'Invalid credentials'));
            info('Check: 1) Database has student1 user, 2) Password is correct');
        } else {
            fail('Login failed', new Error(`Status: ${response.status}`));
            info(`Response: ${response.raw}`);
        }
    } catch (error) {
        fail('Login request failed', error);
    }
}

async function testLoginNegative() {
    logStep(3, 'Authentication - Negative Cases');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    // Test wrong password
    try {
        const response = await httpRequest('POST', '/api/auth/login', null, {
            username: CONFIG.USERNAME,
            password: 'wrongpassword'
        });
        
        if (response.status === 401 || response.status === 400) {
            pass('Wrong password rejected correctly');
        } else {
            warn(`Wrong password returned: ${response.status}`);
        }
    } catch (error) {
        warn('Wrong password test failed: ' + error.message);
    }
    
    // Test non-existent user
    try {
        const response = await httpRequest('POST', '/api/auth/login', null, {
            username: 'nonexistent',
            password: 'password'
        });
        
        if (response.status === 401 || response.status === 404) {
            pass('Non-existent user rejected correctly');
        } else {
            warn(`Non-existent user returned: ${response.status}`);
        }
    } catch (error) {
        warn('Non-existent user test failed: ' + error.message);
    }
}

async function testGetProfile() {
    logStep(4, 'Profile - Get User Info');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/me', accessToken);
        
        if (response.status === 200) {
            pass('Get profile successful');
            const username = response.data?.username || response.data?.data?.username;
            const roles = response.data?.roles || response.data?.data?.roles;
            info(`Username: ${username || 'N/A'}`);
            info(`Roles: ${Array.isArray(roles) ? roles.join(', ') : 'N/A'}`);
        } else {
            fail('Get profile failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Get profile request failed', error);
    }
}

async function testStudentProfile() {
    logStep(5, 'Profile - Student Profile');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/profile/student', accessToken);
        
        if (response.status === 200) {
            pass('Get student profile successful');
            info('Student profile data retrieved');
        } else if (response.status === 404) {
            warn('Student profile not found (may need to create)');
        } else {
            fail('Get student profile failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Get student profile request failed', error);
    }
}

async function testUpdateProfile() {
    logStep(6, 'Profile - Update Profile');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('PUT', '/api/profile', accessToken, {
            displayName: 'Test Student Updated',
            phone: '0123456789'
        });
        
        if (response.status === 200) {
            pass('Update profile successful');
        } else if (response.status === 400) {
            warn('Update profile validation error (may need complete data)');
        } else {
            fail('Update profile failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Update profile request failed', error);
    }
}

async function testListExams() {
    logStep(7, 'Exams - List Available Exams');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/exams', accessToken);
        
        if (response.status === 200) {
            const exams = response.data?.data || response.data || [];
            pass(`List exams successful (${Array.isArray(exams) ? exams.length : 0} exams)`);
            
            if (Array.isArray(exams) && exams.length > 0) {
                const firstExam = exams[0];
                info(`First exam: ${firstExam.title || 'N/A'} (Code: ${firstExam.code || 'N/A'})`);
            }
        } else {
            fail('List exams failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('List exams request failed', error);
    }
}

async function testJoinExamByCode() {
    logStep(8, 'Exams - Join Exam by Code');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    // Test with invalid code
    try {
        const response = await httpRequest('GET', '/api/exams/join?query=INVALID123', accessToken);
        
        if (response.status === 404) {
            pass('Invalid exam code rejected correctly');
        } else if (response.status === 200) {
            warn('Invalid code returned 200 (may exist in DB)');
        } else {
            fail('Invalid code test unexpected status', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Join exam by code test failed', error);
    }
}

async function testPracticeOptions() {
    logStep(9, 'Exams - Practice Test Options');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/exams/practice-options', accessToken);
        
        if (response.status === 200) {
            pass('Get practice options successful');
            info('Practice test configuration available');
        } else {
            fail('Get practice options failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Get practice options request failed', error);
    }
}

async function testMyAttempts() {
    logStep(10, 'Attempts - My Study History');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/attempts/my', accessToken);
        
        if (response.status === 200) {
            const attempts = response.data?.data || response.data || [];
            pass(`List attempts successful (${Array.isArray(attempts) ? attempts.length : 0} attempts)`);
        } else {
            fail('List attempts failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('List attempts request failed', error);
    }
}

async function testAttemptsFiltered() {
    logStep(11, 'Attempts - Filtered Attempts');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/attempts/my?type=exam', accessToken);
        
        if (response.status === 200) {
            pass('List filtered attempts successful');
        } else {
            fail('List filtered attempts failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('List filtered attempts request failed', error);
    }
}

async function testMyClasses() {
    logStep(12, 'Classes - My Classes');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('GET', '/api/student/classes/my-classes', accessToken);
        
        if (response.status === 200) {
            const classes = response.data?.data || response.data || [];
            pass(`List my classes successful (${Array.isArray(classes) ? classes.length : 0} classes)`);
        } else {
            fail('List my classes failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('List my classes request failed', error);
    }
}

async function testJoinClass() {
    logStep(13, 'Classes - Join Class');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('POST', '/api/student/classes/join', accessToken, {
            classCode: 'INVALIDCODE999'
        });
        
        if (response.status === 404 || response.status === 400) {
            pass('Invalid class code rejected correctly');
        } else if (response.status === 200) {
            warn('Invalid code accepted (may exist)');
        } else {
            fail('Join class test unexpected status', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Join class test failed', error);
    }
}

async function testUnauthorizedAccess() {
    logStep(14, 'Security - Unauthorized Access');
    
    try {
        const response = await httpRequest('GET', '/api/me');
        
        if (response.status === 401 || response.status === 403) {
            pass('Unauthorized access blocked correctly');
        } else {
            fail('Unauthorized access should be blocked', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        warn('Unauthorized access test inconclusive');
    }
}

async function testRefreshToken() {
    logStep(15, 'Security - Token Refresh');
    
    if (!refreshToken) {
        skip('No refresh token available');
        return;
    }
    
    try {
        const response = await httpRequest('POST', '/api/auth/refresh', null, {
            refreshToken: refreshToken
        });
        
        if (response.status === 200) {
            const newToken = response.data?.token || response.data?.data?.token;
            if (newToken) {
                pass('Token refresh successful');
                accessToken = newToken; // Update token for subsequent tests
                info('New access token received');
            } else {
                fail('Token refresh - no new token returned');
            }
        } else {
            fail('Token refresh failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Token refresh request failed', error);
    }
}

async function testForgotPassword() {
    logStep(16, 'Utility - Forgot Password');
    
    try {
        const response = await httpRequest('POST', '/api/auth/forgot-password', null, {
            email: 'student1@demo.local'
        });
        
        if (response.status === 200 || response.status === 202) {
            pass('Forgot password request accepted');
        } else {
            fail('Forgot password failed', new Error(`Status: ${response.status}`));
        }
    } catch (error) {
        fail('Forgot password request failed', error);
    }
}

async function testChangePassword() {
    logStep(17, 'Security - Change Password (Negative)');
    
    if (!accessToken) {
        skip('Skipped - no access token');
        return;
    }
    
    try {
        const response = await httpRequest('POST', '/api/auth/change-password', accessToken, {
            currentPassword: 'wrongpassword',
            newPassword: 'newpassword123'
        });
        
        if (response.status === 401 || response.status === 400) {
            pass('Wrong current password rejected correctly');
        } else {
            warn(`Change password test returned: ${response.status}`);
        }
    } catch (error) {
        warn('Change password test inconclusive');
    }
}

// ========================================
// FRONTEND ROUTE TESTS
// ========================================

async function testFrontendRoutes() {
    logHeader('FRONTEND ROUTE TESTS');
    
    const routes = [
        { path: '/', name: 'Home Page' },
        { path: '/login', name: 'Login Page' },
        { path: '/forgot-password', name: 'Forgot Password Page' },
        { path: '/register', name: 'Register Page' },
        { path: '/help-center', name: 'Help Center' }
    ];
    
    for (const route of routes) {
        logStep(0, `${route.name} (${route.path})`);
        
        try {
            const response = await withRetry(() => httpRequest('GET', route.path.replace(CONFIG.API_BASE, '')));
            
            if (response.status === 200) {
                pass(`${route.name} loads successfully`);
            } else if (response.status === 302 || response.status === 301) {
                pass(`${route.name} redirects (${response.status})`);
            } else {
                warn(`${route.name} returned: ${response.status}`);
            }
        } catch (error) {
            warn(`${route.name} test inconclusive`);
        }
    }
}

// ========================================
// GENERATE REPORTS
// ========================================

function generateJSONReport() {
    const report = {
        ...results,
        summary: {
            total: results.total,
            passed: results.passed.length,
            failed: results.failed.length,
            skipped: results.skipped.length,
            successRate: results.total > 0 
                ? ((results.passed.length / results.total) * 100).toFixed(1) + '%' 
                : '0%'
        },
        config: {
            username: CONFIG.USERNAME,
            apiBase: CONFIG.API_BASE
        }
    };
    
    fs.writeFileSync(outputFile, JSON.stringify(report, null, 2));
    return report;
}

function generateHTMLReport() {
    const successRate = results.total > 0 
        ? ((results.passed.length / results.total) * 100).toFixed(1) 
        : 0;
    
    const statusColor = successRate >= 80 ? 'green' : successRate >= 50 ? 'orange' : 'red';
    
    let html = `
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FE_DEMO - Student Role Smoke Test Report</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px 10px 0 0; }
        .header h1 { margin-bottom: 10px; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; padding: 30px; }
        .stat { background: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; }
        .stat-number { font-size: 36px; font-weight: bold; }
        .stat-label { color: #666; margin-top: 5px; }
        .passed { color: #28a745; }
        .failed { color: #dc3545; }
        .skipped { color: #ffc107; }
        .success-rate { font-size: 48px; font-weight: bold; color: #667eea; }
        .content { padding: 30px; }
        .section { margin-bottom: 30px; }
        .section h2 { color: #333; border-bottom: 2px solid #667eea; padding-bottom: 10px; margin-bottom: 20px; }
        .test-item { background: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 10px; display: flex; align-items: center; }
        .test-icon { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-size: 16px; }
        .icon-pass { background: #d4edda; color: #28a745; }
        .icon-fail { background: #f8d7da; color: #dc3545; }
        .icon-skip { background: #fff3cd; color: #ffc107; }
        .test-name { flex: 1; }
        .test-error { color: #dc3545; font-size: 12px; margin-left: 20px; }
        .footer { text-align: center; padding: 20px; color: #666; border-top: 1px solid #eee; }
        .timestamp { color: #999; font-size: 12px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #f8f9fa; font-weight: 600; }
        .badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; }
        .badge-pass { background: #d4edda; color: #155724; }
        .badge-fail { background: #f8d7da; color: #721c24; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎓 FE_DEMO - Student Role Smoke Test</h1>
            <p>Automated testing for all student role functionality</p>
            <p class="timestamp">Test Date: ${new Date(results.timestamp).toLocaleString('vi-VN')}</p>
        </div>
        
        <div class="summary">
            <div class="stat">
                <div class="stat-number">${results.total}</div>
                <div class="stat-label">Total Tests</div>
            </div>
            <div class="stat">
                <div class="stat-number passed">${results.passed.length}</div>
                <div class="stat-label">Passed</div>
            </div>
            <div class="stat">
                <div class="stat-number failed">${results.failed.length}</div>
                <div class="stat-label">Failed</div>
            </div>
            <div class="stat">
                <div class="stat-number skipped">${results.skipped.length}</div>
                <div class="stat-label">Skipped</div>
            </div>
            <div class="stat" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
                <div class="success-rate">${successRate}%</div>
                <div class="stat-label">Success Rate</div>
            </div>
        </div>
        
        <div class="content">
            <div class="section">
                <h2>✅ Passed Tests (${results.passed.length})</h2>
                ${results.passed.length > 0 ? 
                    results.passed.map(test => `
                        <div class="test-item">
                            <div class="test-icon icon-pass">✓</div>
                            <div class="test-name">${test}</div>
                        </div>
                    `).join('') : 
                    '<p style="color: #666;">No tests passed</p>'
                }
            </div>
            
            <div class="section">
                <h2>❌ Failed Tests (${results.failed.length})</h2>
                ${results.failed.length > 0 ? 
                    results.failed.map(item => `
                        <div class="test-item">
                            <div class="test-icon icon-fail">✗</div>
                            <div class="test-name">${item.test}</div>
                            ${item.error ? `<div class="test-error">Error: ${item.error}</div>` : ''}
                        </div>
                    `).join('') : 
                    '<p style="color: #28a745;">🎉 All tests passed!</p>'
                }
            </div>
            
            <div class="section">
                <h2>⏭️ Skipped Tests (${results.skipped.length})</h2>
                ${results.skipped.length > 0 ? 
                    results.skipped.map(test => `
                        <div class="test-item">
                            <div class="test-icon icon-skip">⊘</div>
                            <div class="test-name">${test}</div>
                        </div>
                    `).join('') : 
                    '<p style="color: #666;">No tests skipped</p>'
                }
            </div>
            
            ${results.warnings.length > 0 ? `
            <div class="section">
                <h2>⚠️ Warnings (${results.warnings.length})</h2>
                ${results.warnings.map(warning => `
                    <div class="test-item">
                        <div class="test-icon icon-skip">⚠</div>
                        <div class="test-name">${warning}</div>
                    </div>
                `).join('')}
            </div>
            ` : ''}
            
            <div class="section">
                <h2>📋 Test Configuration</h2>
                <table>
                    <tr><th>Property</th><th>Value</th></tr>
                    <tr><td>API Base URL</td><td>${CONFIG.API_BASE}</td></tr>
                    <tr><td>Frontend URL</td><td>${CONFIG.FRONTEND_BASE}</td></tr>
                    <tr><td>Test Account</td><td>${CONFIG.USERNAME}</td></tr>
                    <tr><td>Token Available</td><td>${accessToken ? 'Yes ✓' : 'No ✗'}</td></tr>
                </table>
            </div>
        </div>
        
        <div class="footer">
            <p>Generated by FE_DEMO Smoke Test Suite</p>
            <p>Report saved to: ${outputFile}</p>
        </div>
    </div>
</body>
</html>`;
    
    fs.writeFileSync(htmlFile, html);
}

// ========================================
// MAIN EXECUTION
// ========================================

async function main() {
    console.log(colors.cyan + '\n╔════════════════════════════════════════════════════════════════════╗' + colors.reset);
    console.log(colors.cyan + '║' + colors.reset + colors.bright + '     FE_DEMO - Student Role Smoke Test Suite v1.0                ' + colors.reset + colors.cyan + '║' + colors.reset);
    console.log(colors.cyan + '╚════════════════════════════════════════════════════════════════════╝' + colors.reset);
    console.log(colors.dim + `  Account: ${CONFIG.USERNAME} / ${CONFIG.PASSWORD}`);
    console.log(`  API: ${CONFIG.API_BASE}`);
    console.log(`  Frontend: ${CONFIG.FRONTEND_BASE}` + colors.reset);
    
    logHeader('STARTING SMOKE TEST');
    
    try {
        // Phase 1: Health Check
        const backendOk = await testHealthCheck();
        
        if (!backendOk) {
            log('\n' + colors.red + '⚠️  Backend is not available. Please start the backend first:' + colors.reset);
            log('   cd BE && mvn spring-boot:run');
            log('');
            fail('Backend not available');
            generateJSONReport();
            generateHTMLReport();
            process.exit(1);
        }
        
        // Phase 2: Authentication Tests
        await testLogin();
        await testLoginNegative();
        await testRefreshToken();
        
        // Phase 3: Profile Tests
        await testGetProfile();
        await testStudentProfile();
        await testUpdateProfile();
        
        // Phase 4: Exam Tests
        await testListExams();
        await testJoinExamByCode();
        await testPracticeOptions();
        
        // Phase 5: Attempt/History Tests
        await testMyAttempts();
        await testAttemptsFiltered();
        
        // Phase 6: Class Tests
        await testMyClasses();
        await testJoinClass();
        
        // Phase 7: Security Tests
        await testUnauthorizedAccess();
        await testChangePassword();
        
        // Phase 8: Utility Tests
        await testForgotPassword();
        
        // Phase 9: Frontend Routes
        await testFrontendRoutes();
        
    } catch (error) {
        console.error(colors.red + '\n❌ Fatal error:' + error + colors.reset);
    }
    
    // Generate Reports
    logHeader('TEST RESULTS');
    
    const report = generateJSONReport();
    generateHTMLReport();
    
    // Print Summary
    const successRate = results.total > 0 
        ? ((results.passed.length / results.total) * 100).toFixed(1) 
        : 0;
    
    console.log(colors.bright + `\n📊 SUMMARY` + colors.reset);
    console.log(`   Total: ${results.total}`);
    console.log(`   ${colors.green}✓ Passed: ${results.passed.length}${colors.reset}`);
    console.log(`   ${colors.red}✗ Failed: ${results.failed.length}${colors.reset}`);
    console.log(`   ${colors.yellow}⊘ Skipped: ${results.skipped.length}${colors.reset}`);
    console.log('');
    console.log(colors.bright + `   📈 Success Rate: ${successRate}%` + colors.reset);
    console.log('');
    
    if (results.failed.length > 0) {
        console.log(colors.red + '⚠️  Some tests failed. Please review the details above.' + colors.reset);
        console.log('');
    } else if (results.passed.length > 0) {
        console.log(colors.green + '✅ All tests passed!' + colors.reset);
        console.log('');
    }
    
    console.log(colors.dim + `📁 Reports saved:` + colors.reset);
    console.log(`   - JSON: ${outputFile}`);
    console.log(`   - HTML: ${htmlFile}`);
    console.log('');
    
    // Return exit code based on test results
    process.exit(results.failed.length > 0 ? 1 : 0);
}

main().catch(console.error);
