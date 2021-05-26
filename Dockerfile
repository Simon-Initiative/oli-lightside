FROM openjdk:8

RUN mkdir /lightside

RUN mkdir -p /oli/repository

COPY lightside/ /lightside/

RUN chmod +x /lightside/runserver.sh

WORKDIR /lightside/

EXPOSE 8000

ENTRYPOINT /lightside/runserver.sh
