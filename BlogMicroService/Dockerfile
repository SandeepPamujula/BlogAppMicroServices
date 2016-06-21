# Extend vert.x image
FROM vertx/vertx3

#                                                       
ENV VERTICLE_NAME com.cisco.BlogMicroService.BlogMicroVerticle
ENV VERTICLE_FILE target/BlogMicroService-0.0.1-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8082

# Copy your verticle to the container                   
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
#CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/* -cluster -cluster-host 173.36.54.105"]
