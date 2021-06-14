FROM openjdk:8

RUN mkdir /lightside

RUN mkdir -p /models

COPY lightside/ /lightside/

RUN chmod +x /lightside/runserver.sh

WORKDIR /lightside/

EXPOSE 8000 9010 9011

ENTRYPOINT /lightside/runserver.sh
