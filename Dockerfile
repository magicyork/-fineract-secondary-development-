# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

FROM azul/zulu-openjdk-alpine:26

LABEL maintainer="Aleksandar Vidakovic <aleks@apache.org>"

WORKDIR /app

COPY fineract-provider/build/libs/fineract-provider-*.jar /app/app.jar

COPY build/fineract/plugins/ /app/plugins/

RUN mkdir -p /tmp /var/logs/fineract

USER nobody:nogroup

EXPOSE 8080 8443

ENTRYPOINT ["java", \
    "-Duser.home=/tmp", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=UTC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dloader.path=/app/plugins/", \
    "-jar", \
    "/app/app.jar"]