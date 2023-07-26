package org.example;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import verticle.DBVerticle;
import verticle.HTTPClientVerticle;
import verticle.HTTPServerVerticle;

public class StudentResultApplication {
    public static void main(String[] args) {
        // Set up cluster manager for Vert.x
        ClusterManager clusterManager = new HazelcastClusterManager();

        // Vert.x options with clustered configuration
        VertxOptions vertxOptions = new VertxOptions().setClusterManager(clusterManager);

        // Create the Vert.x instance
        Vertx.clusteredVertx(vertxOptions, res -> {
            if (res.succeeded()){
                Vertx vertx = res.result();
                vertx.deployVerticle(new HTTPServerVerticle());
                vertx.deployVerticle(new DBVerticle());
                vertx.deployVerticle(new HTTPClientVerticle());

            }else{
                LoggerFactory.getLogger(StudentResultApplication.class).error("Failed to create Vert.x instance.");
            }
        });
    }

}
