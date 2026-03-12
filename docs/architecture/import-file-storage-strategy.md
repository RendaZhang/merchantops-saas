# Import File Storage Strategy

Last updated: 2026-03-12

## Purpose

This note captures the intended file-storage boundary for the Week 5 import backbone before the first import implementation lands.

## Current Decision

For the first import-job slice:

- uploaded import files should go through a replaceable storage abstraction
- the first concrete implementation should use local filesystem storage
- object storage should be deferred until later

## Why

- local storage keeps the first Week 5 slice small and easy to run in a portfolio or open-source setup
- the import workflow can become real without immediately adding S3, MinIO, OSS, or extra deployment complexity
- a storage abstraction keeps the public API and import-job model stable when object storage is introduced later

## Implementation Boundary

- controllers and import services should depend on a storage service interface, not direct filesystem calls
- the database should store a stable `storageKey` and related metadata, not a machine-specific absolute path
- import workers should read files back through the same abstraction
- the local implementation may store files under an application-controlled directory such as `data/imports/`

## Deferred On Purpose

The first Week 5 slice should not require:

- S3 or compatible object storage
- presigned upload URLs
- bucket lifecycle rules
- CDN or external file-delivery concerns
- multi-part large-file optimization

Those can be added later once the async import workflow itself is proven.

## Future Direction

When the import backbone is stable enough, the local storage implementation can be replaced or supplemented by an object-storage implementation without changing:

- the public import-job API
- the import worker contract
- the core `import_job` persistence model
