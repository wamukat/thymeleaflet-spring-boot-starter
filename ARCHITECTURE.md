# Thymeleaflet Spring Boot Starter - Architecture

## Clean Architecture Migration (Phase 8)

This project has been migrated to Clean Architecture with UseCase pattern following DDD principles.

### Architecture Layers

```
📁 src/main/java/io/github/wamukat/thymeleaflet/
├── 🎯 application/
│   ├── port/
│   │   └── inbound/           # UseCase Interfaces (Inbound Ports)
│   │       ├── FragmentDiscoveryUseCase.java
│   │       ├── FragmentPreviewUseCase.java
│   │       ├── FragmentValidationUseCase.java
│   │       └── StoryManagementUseCase.java
│   └── service/               # UseCase Implementations
│       ├── FragmentDiscoveryUseCaseImpl.java
│       ├── FragmentPreviewUseCaseImpl.java
│       ├── FragmentValidationUseCaseImpl.java
│       ├── StoryManagementUseCaseImpl.java
│       └── FragmentStoryApplicationService.java    # Foundation Service
├── 🏗️ domain/                   # Domain Layer
│   ├── model/                 # Domain Models
│   ├── service/               # Domain Services
│   └── port/                  # Domain Ports
└── 🔧 infrastructure/           # Infrastructure Layer
    ├── web/controller/        # Controllers (Adapters)
    ├── discovery/             # Fragment Discovery Services
    ├── rendering/             # Template Rendering
    ├── security/              # Security Components
    └── configuration/         # Spring Configuration
```

### Migration History

- **Phase 8.1**: UseCase Port definitions
- **Phase 8.2**: FragmentPreviewApplicationService → UseCase
- **Phase 8.3**: Controller integration and navigation fixes
- **Phase 8.4**: FragmentDiscoveryApplicationService → UseCase
- **Phase 8.5**: FragmentValidationApplicationService → UseCase  
- **Phase 8.6**: StoryManagementApplicationService → UseCase
- **Phase 8.7**: Final ApplicationService cleanup and Clean Architecture completion
- **Phase 9**: Final integration and performance optimization

### Key Features

✅ **Clean Architecture**: Clear separation of concerns with defined layers
✅ **UseCase Pattern**: Business logic encapsulated in UseCase implementations
✅ **DDD Principles**: Domain-driven design with rich domain models
✅ **Dependency Inversion**: Dependencies flow inward toward domain
✅ **SOLID Principles**: Single responsibility, open/closed, interface segregation
✅ **Testability**: Highly testable with dependency injection
✅ **Performance**: Optimized transaction management and caching

### UseCase Responsibilities

1. **FragmentDiscoveryUseCase**: Fragment search, statistics, hierarchical structure
2. **FragmentPreviewUseCase**: Fragment rendering, preview generation, JavaDoc processing
3. **FragmentValidationUseCase**: Input validation, error handling, metrics logging
4. **StoryManagementUseCase**: Story CRUD operations, parameter management

### Benefits Achieved

- 🎯 **Better Testability**: Each UseCase can be tested independently
- 🔄 **Maintainability**: Clear boundaries and responsibilities
- 📈 **Scalability**: Easy to extend with new UseCases
- 🛡️ **Robustness**: Error handling and validation at UseCase level
- 🚀 **Performance**: Optimized transactions and reduced coupling