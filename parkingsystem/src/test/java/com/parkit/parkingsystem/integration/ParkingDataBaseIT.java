package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import com.parkit.parkingsystem.service.FareCalculatorService;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

  private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
  private static ParkingSpotDAO parkingSpotDAO;
  private static TicketDAO ticketDAO;
  private static DataBasePrepareService dataBasePrepareService;

  @Mock
  private static InputReaderUtil inputReaderUtil;

  @BeforeAll
  private static void setUp() throws Exception {
    parkingSpotDAO = new ParkingSpotDAO();
    parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
    ticketDAO = new TicketDAO();
    ticketDAO.dataBaseConfig = dataBaseTestConfig;
    dataBasePrepareService = new DataBasePrepareService();
  }

  @BeforeEach
  private void setUpPerTest() throws Exception {
    when(inputReaderUtil.readSelection()).thenReturn(1);
    when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    dataBasePrepareService.clearDataBaseEntries();
  }

  @AfterAll
  private static void tearDown() {

  }

  @Test
  public void testParkingACar() {
    ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    parkingService.processIncomingVehicle();
    // Check that a ticket is actually saved in the database and parking table is updated with availability
    Ticket ticket = ticketDAO.getTicket("ABCDEF");
    assertNotNull(ticket, "Ticket should not be null");
    assertEquals("ABCDEF", ticket.getVehicleRegNumber(), "Vehicle registration number should match the input");
    assertNotNull(ticket.getPrice(), "Price should not be null");
    assertNotNull(ticket.getInTime(), "In time should not be null");
    assertNull(ticket.getOutTime(), "Out time should be null at parking");
  }

  @Test
  public void testParkingLotExit() {
    testParkingACar();
    ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    // Wait for some time to simulate parking duration
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    parkingService.processExitingVehicle();
    // Check that the fare generated and out time are populated correctly in the database
    Ticket ticket = ticketDAO.getTicket("ABCDEF");
    assertNotNull(ticket, "Ticket should not be null");
    assertEquals("ABCDEF", ticket.getVehicleRegNumber(), "Vehicle registration number should match the input");
    assertNotNull(ticket.getPrice(), "Price should not be null");
    assertNotNull(ticket.getInTime(), "In time should not be null");
    assertNotNull(ticket.getOutTime(), "Out time should not be null");
  }

  @Test
  public void testParkingLotExitRecurringUser() throws Exception {
    // Park the vehicle for the first time
    ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    parkingService.processIncomingVehicle();

    // Reset the input reader to create a new ticket for the same vehicle
    // when(inputReaderUtil.readSelection()).thenReturn(1);
    // when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

    ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();
    Date inTimeCar = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
    Ticket ticket = new Ticket();
    ticket.setInTime(inTimeCar);
    ticket.setVehicleRegNumber("ABCDEF");
    ticket.setParkingSpot(parkingSpot);
    ticketDAO.saveTicket(ticket);

    parkingService.processExitingVehicle();

    // Simulate vehicle exit after some time
    Date outTimeCar = new Date(System.currentTimeMillis() - (30 * 60 * 1000)); // Exit after 30 minutes
    ticket.setOutTime(outTimeCar);
    FareCalculatorService fareCalculatorService = new FareCalculatorService();
    fareCalculatorService.calculateFare(ticket);
    ticketDAO.updateTicket(ticket);

    // Check the price for the exiting ticket
    Ticket ticketRetrieve = ticketDAO.getTicket("ABCDEF");

    // Calculate the duration of time between the ticket entry and exit times in hours
    long durationMillis = ticketRetrieve.getOutTime().getTime() - ticketRetrieve.getInTime().getTime();
    double durationHours = durationMillis / (1000.0 * 60.0 * 60.0);

    // Check that the ticket price is equal to 1 hour * rate per hour for car * 95% == ticketRetrieve.getPrice()
    double expectedPrice = durationHours * Fare.CAR_RATE_PER_HOUR * 0.95;
    double actualPrice = ticketRetrieve.getPrice();
    assertEquals(expectedPrice, actualPrice, 0.01, "The ticket price should match the expected price");
    // Check that the number of tickets retrieved for the same vehicle ('ABCDEF') is greater than 1
    int ticketCount = ticketDAO.getNbTicket("ABCDEF");
    assertTrue(ticketCount > 1, "The number of tickets retrieved for the same vehicle should be greater than 1");
  }
}
