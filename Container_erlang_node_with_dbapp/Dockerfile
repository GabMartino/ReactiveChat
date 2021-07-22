# Build stage 0
FROM erlang:alpine

# Set working directory
RUN mkdir /ReactiveChatDBErlang
RUN mkdir /ReactiveChatDBErlang/src
RUN mkdir /ReactiveChatDBErlang/target
RUN chmod 777 /ReactiveChatDBErlang/target

WORKDIR /ReactiveChatDBErlang

# Copy our Erlang test application
COPY ReactiveChatDBErlang/ /ReactiveChatDBErlang/




