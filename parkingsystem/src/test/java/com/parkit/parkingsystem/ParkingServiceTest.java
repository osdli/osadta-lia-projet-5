package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {
    private static ParkingService parkingService;
    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @BeforeEach
    private void setUpPerTest() {
        try {
            Mockito.lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            Mockito.lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            Mockito.lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            Mockito.lenient().when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
            Mockito.lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }
    @Test
    public void processExitingVehicleTest(){
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }
   
    
    @Test
    public void testProcessIncomingVehicle() {
        //Given
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        Mockito.lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        Mockito.lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(false);
        //When
        parkingService.processIncomingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }
    @Test
    public void processExitingVehicleTestUnableUpdate() {
        //Given
        Mockito.lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        //When
        parkingService.processExitingVehicle();
        //Then
        verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }
    @Test
    public void testGetNextParkingNumberIfAvailable() {
        //Given
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        Mockito.lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        //When
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        //Then
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertEquals(parkingSpot.getId(), result.getId());
    }
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        // Given
        Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(2);//mock type bike
        Mockito.lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
        // When
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        // Then
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertNull(result);
}

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
      // Given
       Mockito.lenient().when(inputReaderUtil.readSelection()).thenReturn(3);
       // When
       ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
       //Then
       verify(parkingSpotDAO, times(0)).getNextAvailableSlot(any(ParkingType.class));
       assertNull(result);
}
 
}