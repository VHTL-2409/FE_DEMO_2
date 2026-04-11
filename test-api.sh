#!/bin/bash
# FE_DEMO - Student Role API Test Script
# Run with: bash test-api.sh
# Or: ./test-api.sh (after chmod +x)
#
# Prerequisites:
# 1. Backend running at http://localhost:8082
# 2. Frontend running at http://localhost:5173 (for reference)
#
# Test Account:
# Username: student1
# Password: 123456

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
CYAN='\033[0;36m'

# Configuration
API_URL="http://localhost:8082"
FRONTEND_URL="http://localhost:5173"
USERNAME="student1"
PASSWORD="123456"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0
TOTAL_TESTS=0

# Token storage
ACCESS_TOKEN=""
REFRESH_TOKEN=""

# Result file
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="test-results/api-test-results-${TIMESTAMP}.txt"
mkdir -p test-results

# Initialize results file
cat > "$RESULTS_FILE" << EOF
================================================================================
FE_DEMO - Student Role API Test Results
================================================================================
Date: $(date)
Account: $USERNAME / $PASSWORD
API URL: $API_URL
================================================================================

EOF

# Helper functions
log_header() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

log_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
    echo "[TEST] $1" >> "$RESULTS_FILE"
}

log_pass() {
    echo -e "${GREEN}  ✅ PASS:${NC} $1"
    echo "  ✅ PASS: $1" >> "$RESULTS_FILE"
    ((TESTS_PASSED++))
    ((TOTAL_TESTS++))
}

log_fail() {
    echo -e "${RED}  ❌ FAIL:${NC} $1"
    echo "  ❌ FAIL: $1" >> "$RESULTS_FILE"
    ((TESTS_FAILED++))
    ((TOTAL_TESTS++))
}

log_skip() {
    echo -e "${YELLOW}  ⏭️  SKIP:${NC} $1"
    echo "  ⏭️  SKIP: $1" >> "$RESULTS_FILE"
    ((TESTS_SKIPPED++))
    ((TOTAL_TESTS++))
}

log_info() {
    echo -e "${YELLOW}  ℹ️  INFO:${NC} $1"
    echo "  ℹ️  INFO: $1" >> "$RESULTS_FILE"
}

log_api_response() {
    local status=$1
    local response=$2
    local endpoint=$3
    
    echo "  Status: $status" >> "$RESULTS_FILE"
    echo "  Response: $(echo "$response" | head -c 500)" >> "$RESULTS_FILE"
    
    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        echo -e "  Status: ${GREEN}$status OK${NC}"
    elif [ "$status" -eq 401 ]; then
        echo -e "  Status: ${RED}$status Unauthorized${NC}"
    elif [ "$status" -eq 403 ]; then
        echo -e "  Status: ${RED}$status Forbidden${NC}"
    elif [ "$status" -eq 404 ]; then
        echo -e "  Status: ${YELLOW}$status Not Found${NC}"
    elif [ "$status" -ge 500 ]; then
        echo -e "  Status: ${RED}$status Server Error${NC}"
    else
        echo -e "  Status: ${YELLOW}$status${NC}"
    fi
}

# Check if backend is running
check_backend() {
    log_header "Checking Backend Status"
    
    log_test "Backend Health Check"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/api/health" 2>/dev/null)
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Backend is running at $API_URL"
        log_info "Response: $BODY"
        return 0
    else
        log_fail "Backend is not responding (Status: $STATUS)"
        log_info "Please start the backend: cd BE && mvn spring-boot:run"
        return 1
    fi
}

# ========================================
# AUTHENTICATION TESTS
# ========================================

test_login() {
    log_header "Test 1: Authentication - Login"
    
    log_test "POST /api/auth/login with valid credentials"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    echo "$BODY" >> "$RESULTS_FILE"
    log_api_response "$STATUS" "$BODY" "/api/auth/login"
    
    if [ "$STATUS" = "200" ]; then
        # Extract token from response
        ACCESS_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        REFRESH_TOKEN=$(echo "$BODY" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$ACCESS_TOKEN" ]; then
            log_pass "Login successful - Token received"
            log_info "Token: ${ACCESS_TOKEN:0:50}..."
            
            # Save token for later use
            echo "$ACCESS_TOKEN" > .test_token
            return 0
        else
            log_fail "Token not found in response"
            return 1
        fi
    else
        log_fail "Login failed with status $STATUS"
        return 1
    fi
}

test_login_invalid() {
    log_header "Test 2: Authentication - Invalid Login"
    
    log_test "POST /api/auth/login with invalid password"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$USERNAME\",\"password\":\"wrongpassword\"}" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/auth/login (invalid)"
    
    if [ "$STATUS" = "401" ] || [ "$STATUS" = "400" ]; then
        log_pass "Invalid login correctly rejected (Status: $STATUS)"
    else
        log_fail "Expected 401/400, got $STATUS"
    fi
    
    log_test "POST /api/auth/login with non-existent user"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"nonexistentuser\",\"password\":\"password\"}" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    log_api_response "$STATUS" "" "/api/auth/login (nonexistent)"
    
    if [ "$STATUS" = "401" ] || [ "$STATUS" = "404" ]; then
        log_pass "Non-existent user correctly rejected (Status: $STATUS)"
    else
        log_fail "Expected 401/404, got $STATUS"
    fi
}

test_register() {
    log_header "Test 3: Authentication - Register (Skip for existing user)"
    log_skip "Register test - using existing account"
}

# ========================================
# USER PROFILE TESTS
# ========================================

test_get_profile() {
    log_header "Test 4: User Profile - Get Profile"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test profile endpoint"
        return 1
    fi
    
    log_test "GET /api/me"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/me" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/me"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Profile retrieved successfully"
        
        # Extract and display user info
        USERNAME=$(echo "$BODY" | grep -o '"username":"[^"]*"' | head -1 | cut -d'"' -f4)
        ROLES=$(echo "$BODY" | grep -o '"roles":\[[^]]*\]' | head -1)
        
        if [ -n "$USERNAME" ]; then
            log_info "Username: $USERNAME"
        fi
        if [ -n "$ROLES" ]; then
            log_info "Roles: $ROLES"
        fi
    else
        log_fail "Failed to get profile (Status: $STATUS)"
    fi
}

test_student_profile() {
    log_header "Test 5: Student Profile"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test student profile"
        return 1
    fi
    
    log_test "GET /api/profile/student"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/profile/student" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/profile/student"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Student profile retrieved successfully"
    elif [ "$STATUS" = "404" ]; then
        log_info "Student profile not found (may not be created yet)"
    else
        log_fail "Failed to get student profile (Status: $STATUS)"
    fi
}

test_update_profile() {
    log_header "Test 6: Update Profile"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test update profile"
        return 1
    fi
    
    log_test "PUT /api/profile"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API_URL/api/profile" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"displayName":"Test Student","phone":"0123456789"}' 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/profile"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Profile updated successfully"
    elif [ "$STATUS" = "400" ]; then
        log_info "Profile update validation error (Status: 400)"
    else
        log_fail "Failed to update profile (Status: $STATUS)"
    fi
}

test_change_password() {
    log_header "Test 7: Change Password"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test change password"
        return 1
    fi
    
    log_test "POST /api/auth/change-password"
    log_info "Skipping actual password change to preserve test account"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/change-password" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"currentPassword":"wrongpassword","newPassword":"newpassword123"}' 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/auth/change-password (wrong password)"
    
    if [ "$STATUS" = "401" ] || [ "$STATUS" = "400" ]; then
        log_pass "Wrong password correctly rejected"
    else
        log_info "Password change response: $STATUS"
    fi
}

# ========================================
# EXAM TESTS
# ========================================

test_list_exams() {
    log_header "Test 8: List Exams"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test list exams"
        return 1
    fi
    
    log_test "GET /api/exams"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/exams" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/exams"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Exams list retrieved successfully"
        
        # Count exams
        EXAM_COUNT=$(echo "$BODY" | grep -o '"id":' | wc -l)
        log_info "Found $EXAM_COUNT exams"
    else
        log_fail "Failed to get exams (Status: $STATUS)"
    fi
}

test_join_exam_by_code() {
    log_header "Test 9: Join Exam by Code"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test join exam"
        return 1
    fi
    
    log_test "GET /api/exams/join?query=INVALID"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/exams/join?query=INVALID" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/exams/join?query=INVALID"
    
    if [ "$STATUS" = "404" ]; then
        log_pass "Invalid exam code correctly not found"
    elif [ "$STATUS" = "200" ]; then
        log_info "Found exam with code INVALID (may be valid in test data)"
    else
        log_fail "Unexpected status: $STATUS"
    fi
}

test_exam_detail() {
    log_header "Test 10: Get Exam Detail"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test exam detail"
        return 1
    fi
    
    log_test "GET /api/exams/1"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/exams/1" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/exams/1"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Exam detail retrieved successfully"
    elif [ "$STATUS" = "404" ]; then
        log_info "Exam with ID 1 not found"
    else
        log_fail "Failed to get exam detail (Status: $STATUS)"
    fi
}

test_practice_options() {
    log_header "Test 11: Practice Test Options"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test practice options"
        return 1
    fi
    
    log_test "GET /api/exams/practice-options"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/exams/practice-options" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/exams/practice-options"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Practice options retrieved successfully"
    else
        log_fail "Failed to get practice options (Status: $STATUS)"
    fi
}

# ========================================
# ATTEMPT TESTS
# ========================================

test_list_attempts() {
    log_header "Test 12: List My Attempts (Study History)"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test list attempts"
        return 1
    fi
    
    log_test "GET /api/attempts/my"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/attempts/my" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/attempts/my"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Attempts list retrieved successfully"
        
        ATTEMPT_COUNT=$(echo "$BODY" | grep -o '"id":' | wc -l)
        log_info "Found $ATTEMPT_COUNT attempts"
    else
        log_fail "Failed to get attempts (Status: $STATUS)"
    fi
}

test_list_attempts_filtered() {
    log_header "Test 13: List Attempts (Filtered)"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test filtered attempts"
        return 1
    fi
    
    log_test "GET /api/attempts/my?type=exam"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/attempts/my?type=exam" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/attempts/my?type=exam"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Filtered attempts retrieved successfully"
    else
        log_fail "Failed to get filtered attempts (Status: $STATUS)"
    fi
}

# ========================================
# CLASS TESTS
# ========================================

test_my_classes() {
    log_header "Test 14: My Classes"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test my classes"
        return 1
    fi
    
    log_test "GET /api/student/classes/my-classes"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/student/classes/my-classes" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/student/classes/my-classes"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "My classes retrieved successfully"
        
        CLASS_COUNT=$(echo "$BODY" | grep -o '"id":' | wc -l)
        log_info "Found $CLASS_COUNT classes"
    else
        log_fail "Failed to get classes (Status: $STATUS)"
    fi
}

test_join_class_invalid() {
    log_header "Test 15: Join Class (Invalid Code)"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test join class"
        return 1
    fi
    
    log_test "POST /api/student/classes/join with invalid code"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/student/classes/join" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"classCode":"INVALIDCODE123"}' 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/student/classes/join"
    
    if [ "$STATUS" = "404" ] || [ "$STATUS" = "400" ]; then
        log_pass "Invalid class code correctly rejected"
    else
        log_info "Join class response status: $STATUS"
    fi
}

test_class_exams() {
    log_header "Test 16: Get Class Exams"
    
    if [ -z "$ACCESS_TOKEN" ]; then
        log_fail "No access token - cannot test class exams"
        return 1
    fi
    
    log_test "GET /api/student/classes/1/exams"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/student/classes/1/exams" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/student/classes/1/exams"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Class exams retrieved successfully"
    elif [ "$STATUS" = "404" ]; then
        log_info "Class with ID 1 not found or student not enrolled"
    else
        log_fail "Failed to get class exams (Status: $STATUS)"
    fi
}

# ========================================
# UNAUTHORIZED ACCESS TESTS
# ========================================

test_unauthorized_access() {
    log_header "Test 17: Unauthorized Access"
    
    log_test "GET /api/me without token"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/me" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    
    log_api_response "$STATUS" "" "/api/me (no token)"
    
    if [ "$STATUS" = "401" ]; then
        log_pass "Unauthorized access correctly blocked"
    else
        log_fail "Expected 401, got $STATUS"
    fi
    
    log_test "GET /api/exams without token"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/exams" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    
    log_api_response "$STATUS" "" "/api/exams (no token)"
    
    if [ "$STATUS" = "401" ]; then
        log_pass "Protected endpoint correctly blocked"
    else
        log_fail "Expected 401, got $STATUS"
    fi
}

# ========================================
# REFRESH TOKEN TEST
# ========================================

test_refresh_token() {
    log_header "Test 18: Refresh Token"
    
    if [ -z "$REFRESH_TOKEN" ]; then
        log_skip "No refresh token available"
        return
    fi
    
    log_test "POST /api/auth/refresh"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/refresh" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/auth/refresh"
    
    if [ "$STATUS" = "200" ]; then
        log_pass "Token refreshed successfully"
        
        NEW_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$NEW_TOKEN" ]; then
            log_info "New token received: ${NEW_TOKEN:0:50}..."
            ACCESS_TOKEN="$NEW_TOKEN"
            echo "$ACCESS_TOKEN" > .test_token
        fi
    else
        log_fail "Token refresh failed (Status: $STATUS)"
    fi
}

# ========================================
# FORGOT/RESET PASSWORD TESTS
# ========================================

test_forgot_password() {
    log_header "Test 19: Forgot Password"
    
    log_test "POST /api/auth/forgot-password"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/forgot-password" \
        -H "Content-Type: application/json" \
        -d '{"email":"student1@example.com"}' 2>/dev/null)
    
    STATUS=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    log_api_response "$STATUS" "$BODY" "/api/auth/forgot-password"
    
    if [ "$STATUS" = "200" ] || [ "$STATUS" = "202" ]; then
        log_pass "Forgot password request accepted"
    else
        log_info "Forgot password response: $STATUS"
    fi
}

# ========================================
# RESULTS SUMMARY
# ========================================

print_summary() {
    log_header "TEST RESULTS SUMMARY"
    
    echo ""
    echo -e "${BLUE}📊 Total Tests: $TOTAL_TESTS${NC}"
    echo -e "${GREEN}   ✅ Passed: $TESTS_PASSED${NC}"
    echo -e "${RED}   ❌ Failed: $TESTS_FAILED${NC}"
    echo -e "${YELLOW}   ⏭️  Skipped: $TESTS_SKIPPED${NC}"
    echo ""
    
    SUCCESS_RATE=0
    if [ $TOTAL_TESTS -gt 0 ]; then
        SUCCESS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))
    fi
    
    echo -e "${BLUE}📈 Success Rate: $SUCCESS_RATE%${NC}"
    echo ""
    
    # Write to results file
    cat >> "$RESULTS_FILE" << EOF

================================================================================
FINAL SUMMARY
================================================================================
Total Tests: $TOTAL_TESTS
Passed: $TESTS_PASSED
Failed: $TESTS_FAILED
Skipped: $TESTS_SKIPPED
Success Rate: $SUCCESS_RATE%

Results saved to: $RESULTS_FILE
================================================================================
EOF
    
    echo -e "${CYAN}Results saved to: $RESULTS_FILE${NC}"
    echo ""
    
    # Cleanup
    rm -f .test_token
    
    if [ $TESTS_FAILED -gt 0 ]; then
        echo -e "${RED}⚠️  Some tests failed. Please review the results.${NC}"
        return 1
    else
        echo -e "${GREEN}✅ All tests passed!${NC}"
        return 0
    fi
}

# ========================================
# MAIN EXECUTION
# ========================================

main() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════════════════════"
    echo "  FE_DEMO - STUDENT ROLE API TEST SUITE"
    echo "  Account: $USERNAME / $PASSWORD"
    echo "  API URL: $API_URL"
    echo "═══════════════════════════════════════════════════════════════════════════════"
    echo ""
    
    # Check backend availability
    if ! check_backend; then
        echo ""
        echo -e "${RED}❌ Backend is not available. Please start the backend first:${NC}"
        echo "   cd BE && mvn spring-boot:run"
        echo ""
        exit 1
    fi
    
    echo ""
    
    # Run authentication tests
    test_login
    test_login_invalid
    test_register
    
    # Run profile tests
    test_get_profile
    test_student_profile
    test_update_profile
    test_change_password
    
    # Run exam tests
    test_list_exams
    test_join_exam_by_code
    test_exam_detail
    test_practice_options
    
    # Run attempt tests
    test_list_attempts
    test_list_attempts_filtered
    
    # Run class tests
    test_my_classes
    test_join_class_invalid
    test_class_exams
    
    # Run security tests
    test_unauthorized_access
    test_refresh_token
    
    # Run utility tests
    test_forgot_password
    
    # Print summary
    print_summary
}

# Run main function
main "$@"
