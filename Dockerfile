FROM xcgd/libreoffice

RUN apt-get update && apt-get -y -q install ghostscript curl xvfb xfonts-75dpi dos2unix linux-image-extra-virtual xz-utils && \
    curl "https://downloads.wkhtmltopdf.org/0.12/0.12.4/wkhtmltox-0.12.4_linux-generic-amd64.tar.xz" -L -o "wkhtmltopdf.tar.xz" && \
    tar -xvf "wkhtmltopdf.tar.xz" && \
    mv wkhtmltox/bin/wkhtmltopdf /usr/local/bin/wkhtmltopdf && \
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