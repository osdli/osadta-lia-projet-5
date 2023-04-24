package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
    
    public void calculateFare(Ticket ticket) {
    calculateFare(ticket, false);
}

    public void calculateFare(Ticket ticket, boolean discount) {

       
            if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
                throw new IllegalArgumentException("Out time provided is incorrect.");
            }

            // Get the time when the ticket was entered
            long inTime = ticket.getInTime().getTime();

            // Get the time when the ticket was exited
            long outTime = ticket.getOutTime().getTime();

            // Calculate the duration of time between the ticket entry and exit times in hours
            double duration = (double)(outTime - inTime) / (1000 * 60 * 60);

            if (duration < 0.5) {
                ticket.setPrice(0);
                return;
            }

            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
    if (discount) {
    ticket.setPrice (ticket.getPrice() * 0.95);
    }
        }
    }

    
