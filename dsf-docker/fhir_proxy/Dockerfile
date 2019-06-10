FROM httpd:2.4

COPY ./httpd.conf /usr/local/apache2/conf/httpd.conf
COPY ./httpd-ssl.conf /usr/local/apache2/conf/extra/httpd-ssl.conf

COPY ./host.conf /usr/local/apache2/conf/extra/host.conf
COPY ./host-ssl.conf /usr/local/apache2/conf/extra/host-ssl.conf

RUN mkdir /usr/local/apache2/ssl/
RUN chown daemon:daemon /usr/local/apache2/ssl/
RUN chmod 440 /usr/local/apache2/ssl/

VOLUME /usr/local/apache2/ssl/