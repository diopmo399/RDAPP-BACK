// ***********************************************
// This file is loaded automatically before test files
// ***********************************************

// Import commands
import './commands';

// Import Cypress plugins (if installed)
// import 'cypress-axe';
// import '@testing-library/cypress/add-commands';

// Global configuration
Cypress.on('uncaught:exception', (err, runnable) => {
  // Prevent Cypress from failing tests on uncaught exceptions
  // Customize based on your needs
  console.error('Uncaught exception:', err.message);

  // Don't fail the test for these specific errors:
  if (err.message.includes('ResizeObserver loop')) {
    return false;
  }

  // Allow the test to fail for other exceptions
  return true;
});

// Before each test
beforeEach(() => {
  // Clear cookies and localStorage before each test
  // Comment out if you need to preserve state between tests
  // cy.clearCookies();
  // cy.clearLocalStorage();

  // Set viewport (can be overridden in individual tests)
  // cy.viewport(1280, 720);

  // Preserve cookies for session tests
  Cypress.Cookies.defaults({
    preserve: ['session', 'authToken']
  });
});

// After each test
afterEach(function () {
  // Take screenshot on failure
  if (this.currentTest.state === 'failed') {
    const testName = this.currentTest.title.replace(/\s+/g, '-');
    cy.screenshot(`failed-${testName}`);
  }
});

// Global timeout handler
Cypress.on('fail', (error, runnable) => {
  // Log additional context on failure
  cy.log('Test failed:', runnable.title);
  cy.log('Error:', error.message);

  throw error;
});
