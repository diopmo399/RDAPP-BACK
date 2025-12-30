// ***********************************************
// Custom Cypress commands
// ***********************************************

/**
 * Login command
 * Usage: cy.login('user@example.com', 'password')
 */
Cypress.Commands.add('login', (email, password) => {
  cy.session([email, password], () => {
    cy.visit('/login');
    cy.get('[data-cy=email]').type(email);
    cy.get('[data-cy=password]').type(password);
    cy.get('[data-cy=submit]').click();

    // Wait for redirect after login
    cy.url().should('not.include', '/login');

    // Verify logged in state (adjust based on your app)
    cy.getCookie('session').should('exist');
  });
});

/**
 * API login (faster than UI)
 * Usage: cy.apiLogin('user@example.com', 'password')
 */
Cypress.Commands.add('apiLogin', (email, password) => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/auth/login`,
    body: { email, password }
  }).then((response) => {
    expect(response.status).to.eq(200);

    // Store token if using JWT
    if (response.body.token) {
      window.localStorage.setItem('authToken', response.body.token);
    }

    // Store session cookie
    if (response.headers['set-cookie']) {
      cy.setCookie('session', response.headers['set-cookie'][0]);
    }
  });
});

/**
 * Logout command
 * Usage: cy.logout()
 */
Cypress.Commands.add('logout', () => {
  cy.clearCookies();
  cy.clearLocalStorage();
  cy.visit('/');
});

/**
 * Check API health
 * Usage: cy.checkApiHealth()
 */
Cypress.Commands.add('checkApiHealth', () => {
  cy.request({
    method: 'GET',
    url: `${Cypress.env('apiUrl')}/health`,
    failOnStatusCode: false
  }).then((response) => {
    expect(response.status).to.be.oneOf([200, 204]);
  });
});

/**
 * Wait for API response
 * Usage: cy.waitForApi('GET', '/api/users')
 */
Cypress.Commands.add('waitForApi', (method, url, alias = 'apiRequest') => {
  cy.intercept(method, url).as(alias);
  cy.wait(`@${alias}`);
});

/**
 * Get element by data-cy attribute
 * Usage: cy.dataCy('submit-button')
 */
Cypress.Commands.add('dataCy', (value) => {
  return cy.get(`[data-cy=${value}]`);
});

/**
 * Get element by data-testid attribute
 * Usage: cy.dataTestId('submit-button')
 */
Cypress.Commands.add('dataTestId', (value) => {
  return cy.get(`[data-testid=${value}]`);
});

/**
 * Fill form with data
 * Usage: cy.fillForm({ email: 'test@example.com', password: '123' })
 */
Cypress.Commands.add('fillForm', (data) => {
  Object.keys(data).forEach((key) => {
    cy.dataCy(key).clear().type(data[key]);
  });
});

/**
 * Check accessibility with axe
 * Requires: npm install --save-dev cypress-axe axe-core
 * Usage: cy.checkA11y()
 */
Cypress.Commands.add('checkA11y', (context = null, options = null) => {
  cy.injectAxe();
  cy.checkA11y(context, options, (violations) => {
    if (violations.length) {
      cy.task('log', `${violations.length} accessibility violation(s) detected`);
    }
  });
});

/**
 * Take screenshot with timestamp
 * Usage: cy.screenshotWithTimestamp('login-page')
 */
Cypress.Commands.add('screenshotWithTimestamp', (name) => {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  cy.screenshot(`${name}-${timestamp}`);
});

/**
 * Wait for page to be fully loaded
 * Usage: cy.waitForPageLoad()
 */
Cypress.Commands.add('waitForPageLoad', () => {
  cy.window().its('document.readyState').should('eq', 'complete');
});

/**
 * Seed database via API (if available)
 * Usage: cy.seedDatabase('users')
 */
Cypress.Commands.add('seedDatabase', (fixture) => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/test/seed`,
    body: { fixture }
  });
});

/**
 * Clean database via API (if available)
 * Usage: cy.cleanDatabase()
 */
Cypress.Commands.add('cleanDatabase', () => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/test/clean`
  });
});

// ***********************************************
// TypeScript support (optional)
// ***********************************************

// Uncomment if using TypeScript:
// declare global {
//   namespace Cypress {
//     interface Chainable {
//       login(email: string, password: string): Chainable<void>
//       apiLogin(email: string, password: string): Chainable<void>
//       logout(): Chainable<void>
//       checkApiHealth(): Chainable<void>
//       waitForApi(method: string, url: string, alias?: string): Chainable<void>
//       dataCy(value: string): Chainable<JQuery<HTMLElement>>
//       dataTestId(value: string): Chainable<JQuery<HTMLElement>>
//       fillForm(data: Record<string, string>): Chainable<void>
//       checkA11y(context?: any, options?: any): Chainable<void>
//       screenshotWithTimestamp(name: string): Chainable<void>
//       waitForPageLoad(): Chainable<void>
//       seedDatabase(fixture: string): Chainable<void>
//       cleanDatabase(): Chainable<void>
//     }
//   }
// }
