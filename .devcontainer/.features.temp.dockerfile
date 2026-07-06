FROM mcr.microsoft.com/devcontainers/base:ubuntu
COPY --from=jb-devcontainer-features-71c688a5027d56fbcdf5b57f9c4ae2f8 /tmp/jb-devcontainer-features /tmp/jb-devcontainer-features/
ENV SDKMAN_DIR="/usr/local/sdkman"
ENV JAVA_HOME="/usr/local/sdkman/candidates/java/current"
ENV PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
ENV NVM_DIR="/usr/local/share/nvm"
ENV NVM_SYMLINK_CURRENT="true"
ENV DOCKER_BUILDKIT="1"
ENV _CONTAINER_USER="root"
ENV _CONTAINER_USER_HOME="/root"
ENV _REMOTE_USER="vscode"
ENV _REMOTE_USER_HOME="/home/vscode"

ENV SDKMAN_DIR="/usr/local/sdkman"
ENV JAVA_HOME="/usr/local/sdkman/candidates/java/current"
ENV PATH="/usr/local/sdkman/bin:/usr/local/sdkman/candidates/java/current/bin:/usr/local/sdkman/candidates/gradle/current/bin:/usr/local/sdkman/candidates/maven/current/bin:/usr/local/sdkman/candidates/ant/current/bin:${PATH}"
USER root
RUN chmod -R 0755 /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-java-1 \
&& cd /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-java-1 \
&& chmod +x ./devcontainer-feature-setup.sh \
&& ./devcontainer-feature-setup.sh
ENV NVM_DIR="/usr/local/share/nvm"
ENV NVM_SYMLINK_CURRENT="true"
ENV PATH="/usr/local/share/nvm/current/bin:${PATH}"
USER root
RUN chmod -R 0755 /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-node-1 \
&& cd /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-node-1 \
&& chmod +x ./devcontainer-feature-setup.sh \
&& ./devcontainer-feature-setup.sh
ENV DOCKER_BUILDKIT="1"
USER root
RUN chmod -R 0755 /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-docker-in-docker-2 \
&& cd /tmp/jb-devcontainer-features/ghcr.io-devcontainers-features-docker-in-docker-2 \
&& chmod +x ./devcontainer-feature-setup.sh \
&& ./devcontainer-feature-setup.sh