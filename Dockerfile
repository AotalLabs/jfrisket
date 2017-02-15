FROM xcgd/libreoffice

RUN apt-get update && apt-get -y -q install ghostscript wget xvfb xfonts-75dpi dos2unix linux-image-extra-virtual && \
	wget http://download.gna.org/wkhtmltopdf/0.12/0.12.2.1/wkhtmltox-0.12.2.1_linux-trusty-amd64.deb && \
	dpkg -i wkhtmltox-0.12.2.1_linux-trusty-amd64.deb && \
	apt-get -f install
EXPOSE 8080

COPY build/libs/app.jar app.jar

ENTRYPOINT java \
    -Dspring.boot.admin.client.management-url=http://`curl http://rancher-metadata/2015-12-19/self/container/primary_ip`:8081 \
    -Dspring.boot.admin.client.health-url=http://`curl http://rancher-metadata/2015-12-19/self/container/primary_ip`:8081/health \
    -Dspring.boot.admin.client.service-url=http://`curl http://rancher-metadata/2015-12-19/self/container/primary_ip`:8080 \
    -Xmx512m \
    -Xms512m \
    -jar \
     /app.jar