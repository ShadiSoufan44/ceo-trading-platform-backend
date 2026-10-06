Go to Linux machine

```
docker ps
```

Then if you see a sonarqube container in the list, run the following command to remove it:

```
docker rm -f FIRST_TWO_CHARACTERS_OF_CONTAINER_ID
```

Run the `docker` and `curl` commands below **on the
Linux VM**; open the dashboard and run Maven **from Windows**, using the Linux VM's private IP in
place of `PRIVATE_IP`.

```bash
# on the Linux VM
docker run -d --name sonarqube-sprint7 -p 8087:9000 sonarqube:community
```

Takes 1-2 minutes to fully start (it runs its own embedded database on first boot — fine for a
dev/training instance, never for production, exactly like Module 5's KRaft-mode Kafka broker was
fine for training but not how a real broker cluster is usually configured).

```bash
curl -s http://localhost:8087/api/system/status
```

Wait for `"status":"UP"`. From Windows, log in at `http://PRIVATE_IP:8087` with `admin` / `admin` (it will
prompt you to change the password). 

## Run the Real Analysis

Go to `http://PRIVATE_IP:8087`, follow instructions to create a local project.

1. Generate a token
2. Run analysis on your project by copying the command to run the scanner. 
