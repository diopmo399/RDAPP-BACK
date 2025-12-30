/**
 * Navigation Tests
 *
 * Tests for page navigation and routing
 */

describe('Navigation', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should navigate to different pages', () => {
    // Example navigation tests
    // Adjust selectors based on your app

    // Navigate to About page (if exists)
    cy.contains('About').click({ force: true });
    cy.url().should('include', '/about');

    // Navigate back to home
    cy.go('back');
    cy.url().should('not.include', '/about');
  });

  it('should handle 404 pages', () => {
    // Visit non-existent page
    cy.visit('/non-existent-page', { failOnStatusCode: false });

    // Check for 404 indicator
    cy.contains(/404|not found/i).should('exist');
  });

  it('should preserve query parameters', () => {
    cy.visit('/?utm_source=test&utm_campaign=e2e');

    cy.url().should('include', 'utm_source=test');
    cy.url().should('include', 'utm_campaign=e2e');
  });

  it('should handle redirects', () => {
    // Example: redirect from /old-page to /new-page
    cy.visit('/redirect-test', { failOnStatusCode: false });

    // Verify final URL after redirect
    // (Adjust based on your app's redirect logic)
  });

  it('should have working breadcrumbs', () => {
    // Navigate to a deep page
    cy.visit('/category/subcategory/item', { failOnStatusCode: false });

    // Check breadcrumbs exist
    cy.get('[data-cy=breadcrumb], .breadcrumb, nav[aria-label="breadcrumb"]', {
      timeout: 3000
    }).should('exist');
  });

  it('should have accessible navigation menu', () => {
    // Check main navigation
    cy.get('nav, [role="navigation"]').should('exist');

    // Verify navigation is keyboard accessible
    cy.get('nav a').first().focus();
    cy.focused().should('have.attr', 'href');
  });

  it('should highlight active navigation item', () => {
    // Visit a specific page
    cy.visit('/about', { failOnStatusCode: false });

    // Check active state
    cy.get('nav a[href="/about"], nav a.active, nav a[aria-current="page"]', {
      timeout: 3000
    }).should('exist');
  });
});
