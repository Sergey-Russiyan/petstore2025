# Petstore API Test Framework

A comprehensive API testing framework for the Petstore API built with Java, Maven, RestAssured, and Allure reporting.

## Architecture Overview

The framework follows Object-Oriented Programming principles and design patterns:

### Design Patterns Used
- **Singleton Pattern**: `ConfigurationManager` for centralized configuration
- **Template Method Pattern**: `BaseApiClient` and `BaseTest` for common functionality
- **Builder Pattern**: `Order` DTO for flexible object creation
- **Factory Pattern**: Implicit in API client creation

### Project Structure
```
src/
├── main/java/
│   └── com/petstore/
│       ├── client/
│       │   ├── BaseApiClient.java          # Base API client with common config
│       │   └── StoreApiClient.java         # Store-specific API operations
│       ├── config/
│       │   └── ConfigurationManager.java   # Environment configuration
│       ├── dto/
│       │   ├── Order.java                  # Order data transfer object
│       │   ├── OrderStatus.java            # Order status enum
│       │   └── ErrorResponse.java          # Error response DTO
│       ├── framework/
│       │   └── AllureListener.java         # Allure report listener
│       └── util/
│           └── RetryUtils.java             # Retry utility class
│
└── test/
    ├── java/com/petstore/tests/
    │   ├── base/
    │   │   ├── BaseTest.java               # Common test setup and utilities
    │   │   └── BaseStoreTest.java          # Store-specific test foundation
    │   ├── constants/
    │   │   └── TestConstants.java          # Test data constants and configurations
    │   └── store/
    │       ├── StoreOrderTest.java         # Order endpoint test cases
    │       └── StoreInventoryTest.java     # Inventory endpoint test cases
    └── resources/
        ├── log/
        │   └── logback-test.xml            # Logger config
        │
        ├── config/
        │   ├── config.properties           # Default test configuration
        │   ├── dev.properties              # Development environment settings
        │   ├── staging.properties          # Staging environment settings
        │   └── prod.properties             # Production environment settings
        │
        └── testSuites/
             └── petStoreTestSuite.xml       # BE regression suite
```

## Features

### Multi-Environment Support
- Environment-specific configuration files
- Runtime environment switching via system properties
- Configurable timeouts, logging, and base URLs

### Comprehensive Test Coverage
- **GET /store/inventory**: Inventory retrieval and validation
- **POST /store/order**: Order placement with various scenarios
- **GET /store/order/{orderId}**: Order retrieval and error handling
- **DELETE /store/order/{orderId}**: Order deletion

### Advanced Testing Features
- Response time validation
- Header validation (CORS, content-type, server)
- Error response validation
- Complete order lifecycle testing
- Soft assertions for multiple validations

## 🔧 Setup and Installation

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Installation
1. Clone or download the project
2. Navigate to the project directory
3. Install dependencies:
```bash
mvn clean install
```

## Running Tests

### Run All Tests (Default Environment)
```bash
mvn test
```

### Run Tests in Specific Environment
```bash
# Development environment
mvn test -Denv=dev

# Staging environment
mvn test -Denv=staging

# Production environment
mvn test -Denv=prod
```

### Run Specific Test Suites
```bash
# Smoke tests only
mvn test -DsuiteXmlFile=src/test/resources/testng.xml -Dtest.suite="Smoke Tests"

# Regression tests
mvn test -DsuiteXmlFile=src/test/resources/testng.xml -Dtest.suite="Regression Tests"
```

### Run Specific Test Classes
```bash
# Inventory tests only
mvn test -Dtest=StoreInventoryTest

# Order tests only
mvn test -Dtest=StoreOrderTest
```

## Reporting

### Generate Allure Reports
```bash
# Generate and serve Allure report
mvn allure:serve

# Generate Allure report (without serving)
mvn allure:report
```

The Allure report will include:
- Test execution details
- Request/Response logging
- Screenshots and attachments
- Test categorization by Epic/Feature/Story
- Severity levels and descriptions

### Live Report Access (GitHub Pages)
Allure reports are published to GitHub Pages after every workflow run:  
👉 [View Report](https://sergey-russiyan.github.io/petstore2025/)

## Configuration

### Environment Properties
Each environment file supports the following properties:
```properties
# Base URL for the API
base.url=https://petstore.swagger.io/v2

# Timeout configurations (in milliseconds)
request.timeout=30000
connection.timeout=10000

# Logging configurations
log.requests=true
log.responses=true
```

### Adding New Environments
1. Create a new properties file: `src/test/resources/{env-name}.properties`
2. Add environment-specific configurations
3. Run tests with: `mvn test -Denv={env-name}`

## Test Examples

### Basic Scenarios Covered

#### Inventory Tests
- ✅ Successful inventory retrieval
- ✅ Response time validation
- ✅ Header validation (CORS, content-type)
- ✅ Data structure validation
- ✅ Expected pet statuses verification

#### Order Tests
- ✅ Successful order placement
- ✅ Order retrieval for non-existent orders
- ✅ Invalid order ID handling
- ✅ Complete order lifecycle (create → get → delete)
- ✅ Minimal data order creation
- ✅ Future ship date handling
- ✅ Response time validation

### Sample Test Output
```
=================================================
TEST ENVIRONMENT: dev
BASE URL: https://petstore.swagger.io/v2
=================================================
Starting test: testGetInventorySuccess
Starting test: testPlaceOrderSuccess
...
```

## DTOs Usage

### Order DTO Features
- Builder pattern for flexible object creation
- Jackson annotations for JSON serialization/deserialization
- Proper equals/hashCode implementation
- Comprehensive toString method

### Usage Example
```java
Order order = Order.builder()
    .petId(1L)
    .quantity(2)
    .shipDate(LocalDateTime.now())
    .status(OrderStatus.PLACED)
    .complete(true)
    .build();
```

## Troubleshooting

### Common Issues

1. **Tests fail with connection timeout**
  - Check network connectivity
  - Verify base URL in configuration
  - Increase timeout values in properties

2. **Environment not loading**
  - Ensure properties file exists in `src/test/resources/`
  - Check file naming convention: `{env}.properties`
  - Verify system property: `-Denv={env-name}`

3. **Allure report not generating**
  - Ensure Allure is installed: `mvn allure:download`
  - Check if results exist in `target/allure-results/`

## Dependencies

- **RestAssured**: API testing framework
- **TestNG**: Testing framework
- **Allure**: Reporting framework
- **Jackson**: JSON processing
- **SLF4J**: Logging framework

## Contributing

When adding new tests:
1. Extend `BaseTest` for common functionality
2. Use appropriate Allure annotations (`@Epic`, `@Feature`, `@Story`)
3. Follow the established naming conventions
4. Add corresponding DTO classes if needed
5. Update configuration files for new environments  
