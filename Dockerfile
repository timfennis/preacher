FROM openjdk:10-jre

MAINTAINER Tim Fennis <fennis.tim@gmail.com>

WORKDIR app/

COPY build/install ./

EXPOSE 8080

ENTRYPOINT ["./preacher/bin/preacher"]