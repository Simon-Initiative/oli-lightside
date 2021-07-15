# Run on docker swarm mode

## Create local docker registry
```
docker service create --name registry --publish published=5000,target=5000 registry:2
```
## Check if registry service running 
```
docker service ls
```

## Build and push image into registry
```
docker-compose build
docker-compose push
```
## Run on docker swarm
```
docker stack deploy -c docker-compose-swarm.yml lightside_stack
# or in dev mode
docker stack deploy -c docker-compose-swarm-dev.yml lightside_stack
```

## View running swarm service
```
docker stack services lightside_stack
```

## Remove service when done
```
docker stack rm lightside_stack
```

## Track service logs
```
docker service logs -f lightside_stack_lightside
```