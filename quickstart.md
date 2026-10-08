# Setting Up the Development Environment

`git clone` this repository into your workspace that has Java 21 and Maven installed (call this Workspace 1). If you don't have Docker in this workspace, `git clone` into a workspace that does have Docker installed (call this Workspace 2). Note that you can have Workspace 1 and Workspace 2 be the same if you have Java and Docker in the same workspace, just use different terminals.  

## Setup steps
- In Workspace 1 (Java), run `mvn clean package`. 
- In Workspace 2 (Docker), run `docker-compose up`.
- In Workspace 1 (Java), run `mvn spring-boot:run`.