# 개요

signal 서버는 미디어, 웹, 모바일을 연결하여 메시지를 전달하는 역할을 합니다.

# WAS

signal server 는 SpringBoot 내장 Tomcat을 사용

# DB
Redis

# 설정 파일 
 - application.properties 파일에서 test,dev,prod 파일을 설정 할 수있음
 - 각 파일마다 개발서버와, 운영서버에 대한 설정 정보가 기록되어있음

# 배포 방법
형상관리는 Git을 이용하여 관리
배포환경은 Docker를 이용함

1. Docker 설치방법
- 서버에서 혹시모를 도커 삭제
  sudo yum remove docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine
- 설치된 자원 업데이트
  sudo yum -y update
  sudo yum install –y yum-utils
- 도커 repo 설정
  sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker--ce.repo
- 도커 설치
  sudo yum install –y docker-ce docker-ce-cli containerd.io
- 도커 시작
  sudo systemctl start docker
  sudo systemctl enable docker
2. 인텔리제이 IDE에서 tar 파일 생성
- dockerfile, docker-compose.yml 을 작성
- Maven Build해서 war파일 생성
- CMD : docker build -t signal_master .
- CMD : docker save -o signal_master.tar signal_master:latest
3. 서버 배포
- tar파일 서버로 전송 후
- docker load -i signal_master.tar
- docker-compose.yml 파일 서버로 전송 후
- docker-compose up -d
 



  

 

