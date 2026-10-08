# Setting Up the Development Environment

`git clone` this repository into your workspace that has Java 21 and Maven installed (call this Workspace 1). If you don't have Docker in this workspace, `git clone` into a workspace that does have Docker installed (call this Workspace 2). Note that you can have Workspace 1 and Workspace 2 be the same if you have Java and Docker in the same workspace, just use different terminals.  

## Setup steps
- In Workspace 1 (Java), run `mvn clean package`. 
- In Workspace 2 (Docker), run `docker-compose up`. This runs Kafka.  If your docker version is newer it might be `docker compose up`.
    - If Workspace 1 and 2 are different, ideally log into Workspace 2 with Remote SSH Extension in VSCode, from Workspace 1. This will allow VSCode to automatically forward the Kafka port to Workspace 1. 
    - If Workspace 2 is not the same as Workspace 1 AND is not able to be SSHed into from Workspace 1, try searching the internet to figure out how to map the port that Kafka is running on from Workspace 2 (9092) to your Workspace 1. 
- In Workspace 1 (Java), run `mvn spring-boot:run`. This runs the actual backend application, which connects to Kafka. 