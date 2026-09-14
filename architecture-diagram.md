# End-to-End Integration Diagram

```mermaid
flowchart LR
    User[User / API Consumer] -->|Create requirement| WReq[Workflow Request]
    WReq --> WCtrl[AgenticWorkflowController]
    WCtrl --> WSvc[AgenticWorkflowService]
    WSvc --> Graph[Dependency Graph<br/>requirement -> task -> implementation -> testing -> docs -> release-readiness]
    Graph --> Approval[Human approval gate]
    Approval -->|Approved| Release[Release Ready]

    User -->|POST /api/links| SCtrl[ShortUrlController]
    SCtrl --> Svc[ShortUrlService]
    Svc --> Store[(In-memory Link Store)]
    Svc --> Stats[Analytics + Click tracking]
    Stats --> Redirect[GET /{code} redirect]
    Redirect --> Target[Original destination URL]

    Svc -->|Returns link metadata| API[REST API response]
    WSvc --> Metrics[Workflow metrics<br/>success rate, latency, MTTR, retries]
    Metrics --> Observability[Audit / decision trail / status visibility]

    classDef core fill:#dfeeff,stroke:#2d5aa0,color:#102a43;
    classDef gate fill:#fff0c9,stroke:#b77a00,color:#4a2d00;
    classDef data fill:#e7f9e7,stroke:#2e7d32,color:#123b1a;
    class User,WReq,WCtrl,WSvc,SCtrl,Svc,Redirect,Target,API core;
    class Approval,Release gate;
    class Graph,Store,Stats,Metrics,Observability data;
```
