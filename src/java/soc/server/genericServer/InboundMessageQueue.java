/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2010,2015-2016 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2016 Alessandro D'Ottavio
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.server.genericServer;

import java.util.Vector;

import soc.message.SOCMessage;

/**
 *
 * this is one of the main class in the messaging infrastructure implemented in the {@link Server}
 * <P>
 *
 * The main target of this class is to store all the inbound {@link SOCMessage} in text format received from the server by the client connected
 * and solicit the server to  processing the message
 *
 * <P>
 * This class also includes the internal {@link Treater} thread, which processes the
 * messages received in the queue and forwards them to the {@link Server} by calling
 * {@link Server.InboundMessageDispatcher#dispatch(String, StringConnection)}
 * for each inbound message.
 *
 * <P>
 * This queue's constructor only sets up the InboundMessageQueue to receive messages. Afterwards when the
 * {@link Server} is ready to process inbound messages, to start this queue's thread to forward messages
 * into the dispatcher you must call {@link #startMessageProcessing()}.
 *
 * <P>
 * Actually this class is used  by the {@link StringConnection} instances and derived instances classes to store the new message received.<br>
 * The method used to put messages in this queue is {@link #push(String, StringConnection)} <br>
 * the implementation of the  {@link InboundMessageQueue} use an internal thread-safe implementation queue, so it doesn't need to be synchronized
 *
 * <P>
 * The {@link InboundMessageQueue} must be stopped when the {@link Server} owner of this queue is stopped calling {@link #stopMessageProcessing()}.<br>
 * This will stop also the {@link Treater} instance of this queue
 *
 * <UL>
 * <LI> See {@link SOCMessage} for details of the client/server protocol messaging.
 * <LI> See {@link StringConnection} for details of the client/server communication.
 * <LI> See {@link Server.InboundMessageDispatcher#dispatch(String, StringConnection)}
 *      for details of the message processing.
 * </UL>
 *
 * @author Alessandro D'Ottavio
 * @since 2.0.00
 */
public class InboundMessageQueue
{

    /**
     * this queue is used to store the {@link MessageData}
     */
    private Vector<MessageData> inQueue;

    /**
     * the Thread responsible to process the data in the {@link #inQueue}
     */
    private Treater treater;

    /**
     * Message dispatcher at the server which will receive messages from this queue
     */
    private final Server.InboundMessageDispatcher dispatcher;

    /**
     * Create a new InboundMessageQueue. Afterwards when the server is ready
     * to receive messages, you must call {@link #startMessageProcessing()}.
     *
     * @param imd Message dispatcher at the server which will receive messages from this queue
     */
    public InboundMessageQueue(Server.InboundMessageDispatcher imd)
    {
        inQueue = new Vector<MessageData>();
        dispatcher = imd;
    }

    /**
     * Start the {@link Treater} internal thread that calls the server when new messages arrive.
     */
    public void startMessageProcessing()
    {
        treater = new Treater();
        treater.start();
    }

    /**
     * Stop the {@link Treater} internal thread
     */
    public void stopMessageProcessing()
    {
        treater.stopTreater();
    }

    /**
     * Append an element to the end of the inbound queue.
     *<BR>
     *<B>Threads:</B>
     * This notifies the {@link Treater}, waking that thread if it
     * was {@link Object#wait()}ing because the queue was empty.
     *
     * @param receivedMessage from the connection; will never be {@code null}
     * @param clientConnection that send the message; will never be {@code null}
     */
    public void push(String receivedMessage, StringConnection clientConnection)
    {
        synchronized (inQueue)
        {
            inQueue.addElement(new MessageData(receivedMessage, clientConnection));
            inQueue.notify();
        }
    }

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     * Returns as soon as possible; if queue empty, this method doesn't wait until another thread
     * notifies a message has been added.
     *
     * @return the head of this queue, or null if this queue is empty.
     */
    protected final MessageData poll()
    {
        synchronized (inQueue)
        {
            if (inQueue.size() > 0)
                return inQueue.remove(0);
        }

        return null;
    }

    /**
     * Internal class user, a single-threaded reader to process each message stored in the {@link #inQueue}
     * and pass them to the server dispatch.
     *<P>
     * This thread can be stopped calling {@link #stopTreater()}.
     *<P>
     * Before v2.0.00 this class was {@code Server.Treater}.
     *
     * @author Alessandro
     */
    class Treater extends Thread
    {

        /**
         * Is the Treater started and running? Controls the processing of messages:
         * While true, keep looping. When this flag becomes false, Treater's
         * {@link #run()} will exit and end the thread.
         */
        private volatile boolean processMessage;

        public Treater()  // Server parameter is also passed in, since this is an inner class
        {
            setName("treater");  // Thread name for debug
            processMessage = true;
        }

        public void stopTreater()
        {
            processMessage = false;
        }

        public void run()
        {
            while (processMessage)
            {
                MessageData messageData = poll();

                try
                {
                    if (messageData != null)
                        dispatcher.dispatch(messageData.stringMessage, messageData.clientSender);
                }
                catch (Exception e)  // for anything thrown by bugs in server or game code called from dispatch
                {
                    System.out.println("Exception in treater (dispatch) - " + e.getMessage());
                    e.printStackTrace();
                }

                yield();

                synchronized (inQueue)
                {
                    if (inQueue.size() == 0)
                    {
                        try
                        {
                            //D.ebugPrintln("treater waiting");
                            inQueue.wait(1000);  // timeout to help avoid deadlock
                        }
                        catch (Exception ex)
                        {
                            ;   // catch InterruptedException from inQueue.notify() in treat(...)
                        }
                    }
                }
            }
        }
    }


    /**
     * Nested class to store a message's contents and sender.
     * For simplicity and quick access, final fields are used instead of getters.
     */
    private static class MessageData
    {
        /** Message data contents in text format */
        public final String stringMessage;

        /** Client which sent this message */
        public final StringConnection clientSender;

        public MessageData(final String stringMessage, final StringConnection clientSender)
        {
            this.stringMessage = stringMessage;
            this.clientSender = clientSender;
        }

    }


}
