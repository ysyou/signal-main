FROM openjdk:8-jre-alpine

MAINTAINER clamos <clamos.io>

ADD target/signal-1.0.0.jar app.jar

EXPOSE 26460

ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
#docker build -t signal .
#docker save -o signal.tar signal:latest

#police.tar 폴더 이동후
#docker load -i signal.tar

# docker build -t signal . & docker save -o signal.tar signal & docker rmi signal

# 이미지 실행
#sudo docker run -itd -p 15004:15004 --name signal signal

#도커접속
#sudo docker exec -it police /bin/sh

#개발서버
#docker-compose down && docker rmi signal && docker load -i /root/docker/images/signal.tar && docker-compose up -d
#본청
#docker-compose down && docker rmi signal && docker load -i /docker/patrolcam/images/signal.tar && docker-compose up -d




