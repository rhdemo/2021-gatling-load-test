FROM denvazh/gatling:3.2.1

COPY src /load-test/src
COPY target/gatling-kafka/target/ /load-test/target/gatling-kafka/target

WORKDIR /load-test

RUN cp target/gatling-kafka/target/scala-*/gatling-kafka-assembly-*.jar /opt/gatling/lib

RUN mkdir -p /results \
  && chgrp -R 0 /results \
  && chmod -R g+rwX /results \
  && chgrp -R 0 /load-test \
  && chmod -R g+rwX /load-test \
  && chgrp -R 0 /opt/gatling \
  && chmod -R g+rwX /opt/gatling

ENV SIMULATION=BattleSchool

ENTRYPOINT gatling.sh -sf src/test/gatling -rsf src/test/resources -s $SIMULATION -rf /results
