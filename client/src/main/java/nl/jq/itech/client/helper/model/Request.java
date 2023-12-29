package nl.jq.itech.client.helper.model;

import nl.jq.itech.client.helper.SocketHelper;
import nl.jq.itech.client.helper.exception.BadRequestException;

/**
 * Request message
 * <p>
 * A Request is needed to send a message through the SocketHelper to the server
 * The request holds a message and other data, such as:
 * - Max Retries;
 * - On Response Listener;
 * - Reply
 * - Wait For Response (Used for synchronizing and locking)
 */
public class Request {
    private final SocketHelper helper;
    private int maxRetries = 10; // Default to 10 max retries
    private int retries;
    private WaitForResponse waitForResponse;
    private OnResponse onResponse = (success, message) -> false;
    private Message message;
    private Request reply;

    private Request(SocketHelper helper) {
        this.helper = helper;
    }

    public static Builder build(SocketHelper helper) {
        return new Builder(helper);
    }

    public static void sendAndWaitForResponse(Builder builder) {
        final Object lock = new Object();
        builder.lock(() -> {
            synchronized (lock) {
                lock.notify();
            }
        }).syncSend();
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void retry() {
        ++retries;
        send();
    }

    public Message getMessage() {
        return message;
    }

    public OnResponse getOnResponse() {
        return onResponse;
    }

    public WaitForResponse getWaitForRespond() {
        return waitForResponse;
    }

    public void reply() {
        if (reply != null) {
            reply.send();
        }
    }

    /**
     * {@link SocketHelper#send(Request)}
     */
    public void send() {
        if (retries == maxRetries) {
            // If retried too many times, respond with error
            onResponse.response(false, new Message(Command.NONE, "You have retried the command " + retries + " times"));
        } else {
            // Send
            helper.send(this);
        }
    }

    /**
     * {@link SocketHelper#syncSend(Request)}
     */
    public void syncSend() {
        if (retries == maxRetries) {
            // If retried too many times, respond with error
            onResponse.response(false, new Message(Command.NONE, "You have retried the command " + retries + " times"));
        } else {
            // Lock thread while sending
            helper.syncSend(this);
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "message=" + message +
                '}';
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

    public interface WaitForResponse {
        void notifyLock();
    }

    /**
     * Builder class for the response, because it's cooler than using multiple constructors
     */
    public final static class Builder {
        private final Request request;

        /**
         * Create the builder
         *
         * @param helper needed for eventually sending the request
         */
        public Builder(SocketHelper helper) {
            if (helper == null) {
                throw new RuntimeException("Helper should not be null");
            }
            request = new Request(helper);
        }

        /**
         * When the message is send and there is a response,
         * tell the helper what to do by adding a listener
         * Use case could be sending a Request with command USERS
         * On response you would like to use the list you received
         * You could do that with a listener
         * <p>
         * {@link Request.OnResponse#response(boolean, Message)}
         *
         * @param listener listener
         * @return this for chaining
         */
        public Builder setOnResponse(OnResponse listener) {
            request.onResponse = listener;
            return this;
        }

        /**
         * Set the message to be send
         *
         * @param message message
         * @return this for chaining
         */
        public Builder setMessage(Message message) {
            request.message = message;
            return this;
        }

        /**
         * Set the message to be send
         *
         * @param command command
         * @param payload payload
         * @return this for chaining
         */
        public Builder setMessage(Command command, String payload) {
            request.message = new Message(command, payload);
            return this;
        }

        /**
         * Set the command to be send
         *
         * @param command command
         * @return this for chaining
         */
        public Builder setCommand(Command command) {
            if (request.message != null) {
                request.message.setCommand(command);
            } else {
                request.message = new Message(command, "");
            }
            return this;
        }

        /**
         * Add a WaitForResponse lock
         *
         * @param wait lock
         * @return this for chaining
         */
        protected Builder lock(WaitForResponse wait) {
            request.waitForResponse = wait;
            return this;
        }

        /**
         * Set the payload to be send
         *
         * @param payload payload
         * @return this for chaining
         */
        public Builder setPayload(String payload) {
            if (request.message != null) {
                request.message.setPayload(payload);
            } else {
                request.message = new Message(null, payload);
            }
            return this;
        }

        /**
         * After receiving a successful response,
         * reply to it with another request
         *
         * @param request Request to reply with
         * @return this for chaining
         */
        public Builder setReply(Request request) {
            this.request.reply = request;
            return this;
        }

        /**
         * Set the maximum amount of retries
         *
         * @param maxRetries int
         * @return this for chaining
         */
        public Builder setMaxRetries(int maxRetries) {
            request.maxRetries = maxRetries;
            return this;
        }

        /**
         * Create the request
         *
         * @return Request
         */
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

        /**
         * Send this request
         * <p>
         * {@link Builder#create()}
         * {@link Request#send()}
         */
        public void send() {
            create().send();
        }

        /**
         * Send this request synchronized
         * <p>
         * {@link Builder#create()}
         * {@link Request#syncSend()}
         */
        public void syncSend() {
            create().syncSend();
        }
    }
}
