/*
 * Copyright (c) 2014 Cinnober Financial Technology AB, Stockholm,
 * Sweden. All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Cinnober Financial Technology AB, Stockholm, Sweden. You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Cinnober.
 *
 * Cinnober makes no representations or warranties about the suitability
 * of the software, either expressed or implied, including, but not limited
 * to, the implied warranties of merchantibility, fitness for a particular
 * purpose, or non-infringement. Cinnober shall not be liable for any
 * damages suffered by licensee as a result of using, modifying, or
 * distributing this software or its derivatives.
 */

package com.cinnober.exercise.ordermatcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.*;
import java.util.*;

/**
 * Order book with continuous matching of limit orders with time priority.
 *
 * <p>In an electronic exchange an order book is kept: All
 * buy and sell orders are entered into this order book and the prices are
 * set according to specific rules. Bids and asks are matched and trades
 * occur.

 * <p>This class keeps an order book, that can determine in real-time the
 * current market price and combine matching orders to trades. Each order
 * has a quantity and a price.
 *
 * <p><b>The trading rules:</b>
 * It is a match if a buy order exist at a higher price or equal to a sell
 * order in the order book. The quantity of both orders is reduced as much as
 * possible. When an order has a quantity of zero it is removed. An order can
 * match several other orders if the quantity is large enough and the price is
 * correct. The price of the trade is computed as the order that was in the
 * order book first (the passive party).
 *
 * <p>The priority of the orders to match is based on the following:
 * <ol>
 * <li> On the price that is best for the active order (the one just entered)
 * <li> On the time the order was entered (first come first served)
 * </ol>
 *
 * <p><b>Note:</b> some methods are not yet implemented. This is your job!
 * See {@link #addOrder(Order)} and {@link #getOrders(Side)}.
 */
public class OrderMatcher {

    ArrayList<Order> orders;

    /**
     * Create a new order matcher.
     */
    public OrderMatcher() {
        orders = new ArrayList<>();
    }

    /**
     * Add the specified order to the order book.
     *
     * @param order the order to be added, not null. The order will not be modified by the caller after this call.
     * @return any trades that were created by this order, not null.
     */
    public List<Trade> addOrder(Order order) {
        // throw new UnsupportedOperationException("addOrder is not implemented yet"); // FIXME
       switch (order.getSide()) {
        case BUY:
            return handleBuyOrder(order);
        case SELL:
            return handleSellOrder(order);
        default:
            return new ArrayList<Trade>();
       }

    }

    public List<Trade> handleBuyOrder(Order order) {
        ArrayList<Trade> trades = new ArrayList<>();

        Comparator<Order> c = new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                if (o1.getPrice() == o2.getPrice()) {
                    return Long.compare(o2.getId(), o1.getId());
                } else {
                    return Long.compare(o2.getPrice(), o1.getPrice());
                }
            }
        };

        ArrayList<Order> possible = this.orders
            .stream()
            .filter(o -> order.getSide().equals(Side.SELL))
            .filter(o -> o.getPrice() <= order.getPrice())
            .sorted(c)
            .collect(Collectors.toCollection(ArrayList::new));

        System.out.print("NUMBER OF ORDERS: ");
        System.out.println(this.orders.size());
        System.out.print("POSSIBLE TRADES: ");
        System.out.println(possible.size());

        for (Order t : possible) {
            long diff = t.getQuantity() - order.getQuantity();
            if (diff > 0) {
                // t wants to sell more than we wanna buy
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), order.getQuantity()));
                t.setQuantity(diff);
                return trades;
            } else if (diff < 0) {
                // we sell more than t wanna buy
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), t.getQuantity()));
                order.setQuantity(-diff);
                this.orders.remove(t);
            } else {
                // equal
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), order.getQuantity()));
                this.orders.remove(t);
                return trades;
            }
        }
        this.orders.add(order);
        return trades;
    }

    public List<Trade> handleSellOrder(Order order) {
        ArrayList<Trade> trades = new ArrayList<>();
        ArrayList<Order> possible = this.orders
            .stream()
            .filter(o -> order.getSide().equals(Side.BUY))
            .filter(o -> o.getPrice() >= order.getPrice())
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));

        System.out.print("POSSIBLE TRADES: ");
        System.out.println(possible.size());

        for (Order t : possible) {
            long diff = t.getQuantity() - order.getQuantity();
            if (diff > 0) {
                // t wants to buy more
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), order.getQuantity()));
                t.setQuantity(diff);
                return trades;
            } else if (diff < 0) {
                // we sell more than t buys
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), t.getQuantity()));
                order.setQuantity(-diff);
                this.orders.remove(t);
            } else {
                // equal
                trades.add(new Trade(order.getId(), t.getId(), t.getPrice(), order.getQuantity()));
                this.orders.remove(t);
                return trades;
            }
        }
        this.orders.add(order);
        return trades;
    }



    /**
     * Returns all remaining orders in the order book, in priority order, for the specified side.
     *
     * <p>Priority for buy orders is defined as highest price, followed by time priority (first come, first served).
     * For sell orders lowest price comes first, followed by time priority (same as for buy orders).
     *
     * @param side the side, not null.
     * @return all remaining orders in the order book, in priority order, for the specified side, not null.
     */
    public List<Order> getOrders(Side side) {
        // throw new UnsupportedOperationException("getOrders is not implemented yet"); // FIXME
        return this.orders
            .stream()
            .filter(o -> o.getSide().equals(side))
            .collect(Collectors.toList());
    }



    public static void main(String... args) throws Exception {
        OrderMatcher matcher = new OrderMatcher();
        System.out.println("Welcome to the order matcher. Type 'help' for a list of commands.");
        System.out.println();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        LOOP: while ((line=reader.readLine()) != null) {
            line = line.trim();
            try {
                switch(line) {
                    case "help":
                        System.out.println("Available commands: \n"
                                + "  buy|sell <quantity>@<price> [#<id>]  - Enter an order.\n"
                                + "  list                                 - List all remaining orders.\n"
                                + "  quit                                 - Quit.\n"
                                + "  help                                 - Show help (this message).\n");
                        break;
                    case "":
                        // ignore
                        break;
                    case "quit":
                        break LOOP;
                    case "list":
                        System.out.println("BUY:");
                        matcher.getOrders(Side.BUY).stream().map(Order::toString).forEach(System.out::println);
                        System.out.println("SELL:");
                        matcher.getOrders(Side.SELL).stream().map(Order::toString).forEach(System.out::println);
                        break;
                    default: // order
                        matcher.addOrder(Order.parse(line)).stream().map(Trade::toString).forEach(System.out::println);
                        break;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Bad input: " + e.getMessage());
            } catch (UnsupportedOperationException e) {
                System.err.println("Sorry, this command is not supported yet: " + e.getMessage());
            }
        }
        System.out.println("Good bye!");
    }
}
