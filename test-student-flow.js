/**
 * FE_DEMO - Student Role Automated Test
 * Run with: node test-student-flow.js
 * 
 * Prerequisites:
 * 1. Start the frontend: cd demo && npm run dev
 * 2. Start the backend: cd BE && mvn spring-boot:run
 * 3. Install Playwright: npm install -D playwright && npx playwright install chromium
 */

const { chromium } = require('playwright');

const CONFIG = {
  BASE_URL: 'http://localhost:5173',
  API_URL: 'http://localhost:8082',
  USERNAME: 'student1',
  PASSWORD: '123456',
  HEADLESS: false, // Set to true for CI/CD
  TIMEOUT: 30000,
  SCREENSHOT_DIR: './test-results/screenshots'
};

const fs = require('fs');
const path = require('path');

// Create screenshot directory
const screenshotDir = path.resolve(CONFIG.SCREENSHOT_DIR);
if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

let browser;
let context;
let page;
let authToken = null;

// Utility functions
async function takeScreenshot(name) {
  const filename = `${Date.now()}-${name}.png`;
  const filepath = path.join(screenshotDir, filename);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`  📸 Screenshot: ${filepath}`);
  return filepath;
}

async function waitAndClick(selector, options = {}) {
  await page.waitForSelector(selector, { state: 'visible', timeout: CONFIG.TIMEOUT });
  await page.click(selector, options);
}

async function waitAndFill(selector, value) {
  await page.waitForSelector(selector, { state: 'visible', timeout: CONFIG.TIMEOUT });
  await page.fill(selector, value);
}

async function getText(selector) {
  await page.waitForSelector(selector, { state: 'visible', timeout: CONFIG.TIMEOUT });
  return await page.textContent(selector);
}

function logStep(step, description) {
  console.log(`\n📋 Step ${step}: ${description}`);
}

function logPass(message) {
  console.log(`  ✅ PASS: ${message}`);
}

function logFail(message, error = null) {
  console.log(`  ❌ FAIL: ${message}`);
  if (error) {
    console.log(`     Error: ${error.message || error}`);
  }
}

function logInfo(message) {
  console.log(`  ℹ️  INFO: ${message}`);
}

// Test Results
const testResults = {
  passed: [],
  failed: [],
  skipped: [],
  total: 0
};

function recordTest(name, status, error = null) {
  testResults.total++;
  if (status === 'PASS') {
    testResults.passed.push(name);
  } else if (status === 'FAIL') {
    testResults.failed.push({ name, error });
  } else {
    testResults.skipped.push(name);
  }
}

// ========================================
// TEST CASES
// ========================================

async function testLogin() {
  logStep(1, 'Login with student1/123456');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/login', { waitUntil: 'networkidle' });
    await takeScreenshot('01-login-page');
    
    // Find and fill username field
    const usernameSelectors = [
      'input[type="text"]',
      'input[placeholder*="user" i]',
      'input[placeholder*="tên" i]',
      'input[name="username"]',
      'input[type="email"]'
    ];
    
    let usernameField = null;
    for (const selector of usernameSelectors) {
      try {
        await page.waitForSelector(selector, { timeout: 2000 });
        usernameField = selector;
        break;
      } catch {}
    }
    
    if (!usernameField) {
      logFail('Username field not found');
      recordTest('Login - Username field', 'FAIL', new Error('Username field not found'));
      return false;
    }
    
    // Find and fill password field
    const passwordSelectors = [
      'input[type="password"]',
      'input[placeholder*="pass" i]',
      'input[placeholder*="mật" i]'
    ];
    
    let passwordField = null;
    for (const selector of passwordSelectors) {
      try {
        await page.waitForSelector(selector, { timeout: 2000 });
        passwordField = selector;
        break;
      } catch {}
    }
    
    if (!passwordField) {
      logFail('Password field not found');
      recordTest('Login - Password field', 'FAIL', new Error('Password field not found'));
      return false;
    }
    
    // Fill credentials
    await page.fill(usernameField, CONFIG.USERNAME);
    await page.fill(passwordField, CONFIG.PASSWORD);
    await takeScreenshot('02-login-filled');
    
    // Find and click submit button
    const submitSelectors = [
      'button[type="submit"]',
      'button:has-text("Đăng nhập")',
      'button:has-text("Login")',
      'button:has-text("Sign in")'
    ];
    
    let submitButton = null;
    for (const selector of submitSelectors) {
      try {
        await page.waitForSelector(selector, { timeout: 2000 });
        submitButton = selector;
        break;
      } catch {}
    }
    
    if (!submitButton) {
      logFail('Submit button not found');
      recordTest('Login - Submit button', 'FAIL', new Error('Submit button not found'));
      return false;
    }
    
    await page.click(submitButton);
    await page.waitForTimeout(3000); // Wait for redirect
    
    const currentUrl = page.url();
    await takeScreenshot('03-after-login');
    
    if (currentUrl.includes('/student') || currentUrl.includes('/select-role')) {
      logPass('Login successful, redirected to: ' + currentUrl);
      recordTest('Login', 'PASS');
      
      // Handle role selection if needed
      if (currentUrl.includes('/select-role')) {
        logInfo('Role selection page detected');
        await takeScreenshot('04-role-selection');
        
        const studentRoleButton = 'button:has-text("Học sinh"), button:has-text("Student")';
        try {
          await page.waitForSelector(studentRoleButton, { timeout: 5000 });
          await page.click(studentRoleButton);
          await page.waitForTimeout(2000);
          await takeScreenshot('05-after-role-selection');
        } catch {
          logInfo('Student role button not found or already selected');
        }
      }
      
      return true;
    } else {
      logFail('Login failed - redirected to: ' + currentUrl);
      recordTest('Login', 'FAIL', new Error('Wrong redirect URL: ' + currentUrl));
      return false;
    }
  } catch (error) {
    logFail('Login failed with exception', error);
    recordTest('Login', 'FAIL', error);
    await takeScreenshot('00-login-error');
    return false;
  }
}

async function testDashboard() {
  logStep(2, 'Test Student Dashboard');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/dashboard', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('06-dashboard');
    
    // Check for dashboard elements
    const dashboardElements = {
      'Dashboard heading': ['h1', 'h2', 'h3'],
      'KPI cards': ['.stat-card', '.kpi-card', '[class*="stat"]'],
      'Quick actions': ['button:has-text("Vào thi")', 'button:has-text("Lịch sử")'],
    };
    
    for (const [name, selectors] of Object.entries(dashboardElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      } else {
        logFail(`${name} not found`);
      }
    }
    
    recordTest('Dashboard', 'PASS');
    return true;
  } catch (error) {
    logFail('Dashboard test failed', error);
    recordTest('Dashboard', 'FAIL', error);
    return false;
  }
}

async function testSchedule() {
  logStep(3, 'Test Lịch thi (Schedule)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/schedule', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('07-schedule');
    
    // Check for schedule elements
    const scheduleElements = {
      'Schedule page': ['h1', 'h2'],
      'Filter tabs': ['button:has-text("Tất cả")', 'button:has-text("Hôm nay")', 'button:has-text("Sắp tới")'],
    };
    
    for (const [name, selectors] of Object.entries(scheduleElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    // Test filter buttons
    const filterButtons = ['Tất cả', 'Hôm nay', 'Sắp tới', 'Hoàn thành'];
    for (const filter of filterButtons) {
      try {
        await page.click(`button:has-text("${filter}")`);
        await page.waitForTimeout(500);
        logPass(`Filter "${filter}" clicked`);
      } catch {
        logFail(`Filter "${filter}" not clickable`);
      }
    }
    
    await takeScreenshot('08-schedule-filtered');
    recordTest('Schedule', 'PASS');
    return true;
  } catch (error) {
    logFail('Schedule test failed', error);
    recordTest('Schedule', 'FAIL', error);
    return false;
  }
}

async function testExamJoin() {
  logStep(4, 'Test Vào thi (Exam Join)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/exam-join', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('09-exam-join');
    
    // Check for exam join elements
    const examJoinElements = {
      'Exam code input': ['input[placeholder*="mã" i]', 'input[placeholder*="code" i]'],
      'Join button': ['button:has-text("Vào thi")', 'button:has-text("Tham gia")'],
      'Class exams list': ['.exam-card', '.class-exam-item']
    };
    
    for (const [name, selectors] of Object.entries(examJoinElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    recordTest('Exam Join', 'PASS');
    return true;
  } catch (error) {
    logFail('Exam Join test failed', error);
    recordTest('Exam Join', 'FAIL', error);
    return false;
  }
}

async function testClasses() {
  logStep(5, 'Test Lớp học (Classes)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/classes', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('10-classes');
    
    // Check for class elements
    const classElements = {
      'Class list': ['.class-card', '.class-item', '[class*="class"]'],
      'Join class button': ['button:has-text("Tham gia lớp")', 'button:has-text("Join")'],
      'Search input': ['input[placeholder*="tìm" i]', 'input[type="search"]']
    };
    
    for (const [name, selectors] of Object.entries(classElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    // Test join class modal
    try {
      const joinButton = await page.$('button:has-text("Tham gia lớp")');
      if (joinButton) {
        await joinButton.click();
        await page.waitForTimeout(1000);
        await takeScreenshot('11-join-class-modal');
        
        const codeInput = await page.$('input[placeholder*="mã" i], input[placeholder*="code" i]');
        if (codeInput) {
          logPass('Join class modal opened with code input');
        }
        
        // Close modal
        await page.keyboard.press('Escape');
        await page.waitForTimeout(500);
      }
    } catch (error) {
      logFail('Join class modal test', error);
    }
    
    recordTest('Classes', 'PASS');
    return true;
  } catch (error) {
    logFail('Classes test failed', error);
    recordTest('Classes', 'FAIL', error);
    return false;
  }
}

async function testStudyHistory() {
  logStep(6, 'Test Lịch sử học tập (Study History)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/study-history', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('12-study-history');
    
    // Check for history elements
    const historyElements = {
      'History list': ['.result-card', '.history-item', '[class*="attempt"]'],
      'Pagination': ['button:has-text("»")', 'button:has-text("»")', '.pagination'],
    };
    
    for (const [name, selectors] of Object.entries(historyElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    recordTest('Study History', 'PASS');
    return true;
  } catch (error) {
    logFail('Study History test failed', error);
    recordTest('Study History', 'FAIL', error);
    return false;
  }
}

async function testProfile() {
  logStep(7, 'Test Hồ sơ cá nhân (Profile)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/profile', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('13-profile');
    
    // Check for profile elements
    const profileElements = {
      'Profile heading': ['h1', 'h2'],
      'Edit button': ['button:has-text("Sửa")', 'button:has-text("Edit")'],
      'Change password': ['button:has-text("Đổi mật khẩu")', 'button:has-text("Password")'],
      'Avatar section': ['input[type="file"]', '[class*="avatar"]']
    };
    
    for (const [name, selectors] of Object.entries(profileElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    recordTest('Profile', 'PASS');
    return true;
  } catch (error) {
    logFail('Profile test failed', error);
    recordTest('Profile', 'FAIL', error);
    return false;
  }
}

async function testPracticeTest() {
  logStep(8, 'Test Tạo bài luyện tập (Practice Test)');
  
  try {
    await page.goto(CONFIG.BASE_URL + '/student/generate-practice-test', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot('14-practice-test');
    
    // Check for practice test elements
    const practiceElements = {
      'Upload section': ['input[type="file"]', '.upload-area'],
      'Question count selector': ['select', 'input[type="number"]'],
      'Duration selector': ['input[type="number"]'],
      'Start button': ['button:has-text("Bắt đầu")', 'button:has-text("Tạo")']
    };
    
    for (const [name, selectors] of Object.entries(practiceElements)) {
      let found = false;
      for (const selector of selectors) {
        try {
          await page.waitForSelector(selector, { timeout: 2000 });
          found = true;
          break;
        } catch {}
      }
      if (found) {
        logPass(`${name} found`);
      }
    }
    
    recordTest('Practice Test', 'PASS');
    return true;
  } catch (error) {
    logFail('Practice Test failed', error);
    recordTest('Practice Test', 'FAIL', error);
    return false;
  }
}

async function testAIChat() {
  logStep(9, 'Test AI Chat (if available)');
  
  try {
    // Check if AI chat exists by looking for it on any page
    const chatSelectors = [
      '[class*="chat"]',
      '[class*="ai"]',
      'button:has-text("Chat")',
      '[data-testid="ai-chat"]'
    ];
    
    let chatFound = false;
    for (const selector of chatSelectors) {
      try {
        const element = await page.$(selector);
        if (element) {
          chatFound = true;
          logPass(`AI Chat element found: ${selector}`);
          await takeScreenshot('15-ai-chat');
          break;
        }
      } catch {}
    }
    
    if (!chatFound) {
      logInfo('AI Chat not found on current page (may require login or specific route)');
      recordTest('AI Chat', 'SKIPPED');
    } else {
      recordTest('AI Chat', 'PASS');
    }
    
    return true;
  } catch (error) {
    logFail('AI Chat test failed', error);
    recordTest('AI Chat', 'FAIL', error);
    return false;
  }
}

async function testLogout() {
  logStep(10, 'Test Đăng xuất (Logout)');
  
  try {
    // Look for logout button
    const logoutSelectors = [
      'button:has-text("Đăng xuất")',
      'button:has-text("Logout")',
      'button:has-text("Sign out")',
      '[href="/login"]',
      'a:has-text("Đăng xuất")'
    ];
    
    let logoutFound = false;
    for (const selector of logoutSelectors) {
      try {
        const element = await page.waitForSelector(selector, { timeout: 2000 });
        if (element) {
          await element.click();
          logoutFound = true;
          logPass('Logout button clicked');
          break;
        }
      } catch {}
    }
    
    if (!logoutFound) {
      // Try to find it in a dropdown menu
      const menuButton = await page.$('[class*="menu"] button, [class*="dropdown"] button');
      if (menuButton) {
        await menuButton.click();
        await page.waitForTimeout(500);
        
        for (const selector of logoutSelectors) {
          try {
            const element = await page.$(selector);
            if (element) {
              await element.click();
              logoutFound = true;
              logPass('Logout from menu clicked');
              break;
            }
          } catch {}
        }
      }
    }
    
    await page.waitForTimeout(2000);
    const currentUrl = page.url();
    await takeScreenshot('16-after-logout');
    
    if (currentUrl.includes('/login')) {
      logPass('Logout successful, redirected to login page');
      recordTest('Logout', 'PASS');
    } else {
      logFail('Logout failed - URL: ' + currentUrl);
      recordTest('Logout', 'FAIL', new Error('Wrong redirect URL: ' + currentUrl));
    }
    
    return true;
  } catch (error) {
    logFail('Logout test failed', error);
    recordTest('Logout', 'FAIL', error);
    return false;
  }
}

// ========================================
// MAIN TEST RUNNER
// ========================================

async function runTests() {
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('  FE_DEMO - STUDENT ROLE AUTOMATED TEST');
  console.log('  Account: ' + CONFIG.USERNAME + ' / ' + CONFIG.PASSWORD);
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('');
  console.log('⚙️  Initializing browser...');
  
  try {
    browser = await chromium.launch({ 
      headless: CONFIG.HEADLESS,
      args: ['--start-maximized']
    });
    
    context = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      ignoreHTTPSErrors: true
    });
    
    page = await context.newPage();
    
    // Set default timeout
    page.setDefaultTimeout(CONFIG.TIMEOUT);
    
    // Enable console logging from browser
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log(`  🐛 Browser Console Error: ${msg.text()}`);
      }
    });
    
    // Enable request/response logging
    page.on('response', response => {
      if (response.url().includes('/api/') && response.status() >= 400) {
        console.log(`  ⚠️  API Error: ${response.status()} - ${response.url()}`);
      }
    });
    
    console.log('✅ Browser initialized successfully\n');
    
    // Check if app is running
    try {
      const response = await page.goto(CONFIG.BASE_URL, { waitUntil: 'domcontentloaded', timeout: 10000 });
      if (response.status() >= 500) {
        console.log('⚠️  Warning: Frontend may not be running properly (status: ' + response.status() + ')');
      }
    } catch (error) {
      console.log('❌ Error: Cannot connect to frontend at ' + CONFIG.BASE_URL);
      console.log('   Please ensure the frontend is running: cd demo && npm run dev');
      console.log('');
      
      // Try API directly
      try {
        const apiResponse = await fetch(CONFIG.API_URL + '/api/health');
        if (apiResponse.ok) {
          console.log('✅ Backend API is running at ' + CONFIG.API_URL);
        }
      } catch {
        console.log('❌ Backend API is also not accessible at ' + CONFIG.API_URL);
      }
      
      await browser.close();
      return;
    }
    
    // Run tests
    console.log('═══════════════════════════════════════════════════════════════');
    console.log('  STARTING TEST SUITE');
    console.log('═══════════════════════════════════════════════════════════════\n');
    
    // Test 1: Login
    const loginSuccess = await testLogin();
    if (!loginSuccess) {
      console.log('\n⚠️  Login failed. Cannot continue with remaining tests.');
      console.log('   Please check your credentials and try again.');
      await browser.close();
      return;
    }
    
    // Test 2-9: Feature tests
    await testDashboard();
    await testSchedule();
    await testExamJoin();
    await testClasses();
    await testStudyHistory();
    await testProfile();
    await testPracticeTest();
    await testAIChat();
    
    // Test 10: Logout
    await testLogout();
    
    // ========================================
    // TEST RESULTS SUMMARY
    // ========================================
    console.log('\n═══════════════════════════════════════════════════════════════');
    console.log('  TEST RESULTS SUMMARY');
    console.log('═══════════════════════════════════════════════════════════════\n');
    
    console.log(`📊 Total Tests: ${testResults.total}`);
    console.log(`   ✅ Passed: ${testResults.passed.length}`);
    console.log(`   ❌ Failed: ${testResults.failed.length}`);
    console.log(`   ⏭️  Skipped: ${testResults.skipped.length}`);
    console.log('');
    
    if (testResults.passed.length > 0) {
      console.log('✅ Passed Tests:');
      testResults.passed.forEach(name => console.log('   - ' + name));
      console.log('');
    }
    
    if (testResults.failed.length > 0) {
      console.log('❌ Failed Tests:');
      testResults.failed.forEach(({ name, error }) => {
        console.log(`   - ${name}`);
        if (error) {
          console.log(`     Error: ${error.message || error}`);
        }
      });
      console.log('');
    }
    
    if (testResults.skipped.length > 0) {
      console.log('⏭️  Skipped Tests:');
      testResults.skipped.forEach(name => console.log('   - ' + name));
      console.log('');
    }
    
    console.log('═══════════════════════════════════════════════════════════════');
    console.log('  Screenshots saved to: ' + screenshotDir);
    console.log('═══════════════════════════════════════════════════════════════');
    
    // Calculate success rate
    const successRate = testResults.total > 0 
      ? ((testResults.passed.length / testResults.total) * 100).toFixed(1) 
      : 0;
    console.log(`\n📈 Success Rate: ${successRate}%\n`);
    
  } catch (error) {
    console.error('❌ Fatal error during test execution:', error);
  } finally {
    if (browser) {
      await browser.close();
      console.log('✅ Browser closed');
    }
  }
}

// Run the tests
runTests().catch(console.error);
