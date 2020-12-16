package helper.model;

import helper.SocketHelper;
import helper.exception.*;

public class Request {
    private final SocketHelper helper;
    private int maxRetries = 10; // Default to 10 max retries
    private int retries;
    private WaitForRespond waitForRespond;
    private OnResponse onResponse = (success, message) -> false;
    private Message message;
    private Request reply;

    private Request(SocketHelper helper) {
        this.helper = helper;
    }

    public static Builder build(SocketHelper helper) {
        return new Builder(helper);
    }

    public void retry() {
        ++retries;
        send();
    }

    public static void sendAndWaitForResponse(Builder builder) {
        final Object lock = new Object();
        builder.lock(() -> {
            synchronized (lock) {
                lock.notify();
            }
        }).send();
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Message getMessage() {
        return message;
    }

    public OnResponse getOnResponse() {
        return onResponse;
    }

    public WaitForRespond getWaitForRespond() {
        return waitForRespond;
    }

    public void reply() {
        if (reply != null) {
            reply.send();
        }
    }

    public void send() {
        if (retries == maxRetries) {
            onResponse.response(false, new Message(Command.NONE, "You have retried the command " + retries + " times"));
        } else {
            helper.send(this);
        }
    }

    public void syncSend() {
        if (retries == maxRetries) {
            onResponse.response(false, new Message(Command.NONE, "You have retried the command " + retries + " times"));
        } else {
            helper.syncSend(this);
        }
    }

    public interface OnResponse {
        /**
         * Gets called when there is a response for the given request
         * If the request failed you could return true to automatically resend the request and retry.
         *
         * @param success true if request succeeded
         * @return if @{true}, retry request
         */
        boolean response(boolean success, Message message);
    }

    public interface WaitForRespond {
        void notifyLock();
    }

    public final static class Builder {
        private final Request request;

        public Builder(SocketHelper helper) {
            assert helper != null : "Helper should not be null";
            request = new Request(helper);
        }

        public Builder setOnResponse(OnResponse listener) {
            request.onResponse = listener;
            return this;
        }

        public Builder setMessage(Message message) {
            request.message = message;
            return this;
        }

        public Builder setMessage(Command command, String payload) {
            request.message = new Message(command, payload);
            return this;
        }

        public Builder setCommand(Command command) {
            if (request.message != null) {
                request.message.setCommand(command);
            } else {
                request.message = new Message(command, "");
            }
            return this;
        }

        public Builder lock(WaitForRespond wait) {
            request.waitForRespond = wait;
            return this;
        }

        public Builder setPayload(String payload) {
            if (request.message != null) {
                request.message.setPayload(payload);
            } else {
                request.message = new Message(null, payload);
            }
            return this;
        }

        public Builder setReply(Request request) {
            this.request.reply = request;
            return this;
        }

        public Builder setMaxRetries(int maxRetries) {
            request.maxRetries = maxRetries;
            return this;
        }

        public Request create() {
            if (request.message == null) {
                throw new BadRequestException("You have to define a message");
            } else if (request.message.getCommand() == null) {
                throw new BadRequestException("The command is required");
            } else if (request.onResponse == null) {
                throw new BadRequestException("A response is needed");
            } else {
                return request;
            }
        }

        public void send() {
            create().send();
        }

        public void syncSend() {
            create().syncSend();
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "message=" + message +
                '}';
    }
}
