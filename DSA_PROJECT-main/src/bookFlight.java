import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class bookFlight extends JFrame implements ActionListener {
    private static List<Flight> flights;
    private JTable flightTable;
    private JTextField searchField;
    private JComboBox<String> dateFilterCombo, priceFilterCombo, cabinFilterCombo, hourFilterCombo;
    private DefaultTableModel tableModel;
    private JButton applyFilterButton;
    private JButton bookButton;
    private JButton goBackButton;
    private final String userEmail;
    private JTextField fromField;
    private JTextField toField;


    public bookFlight(String userEmail) {
        this.userEmail = userEmail;
        this.setResizable(false);
        setTitle("SKY RESERVE");
        getContentPane().setBackground(Color.decode("#0F149a"));
        initializeFlights();
        createUI();
        setVisible(true);
    }

    private void initializeFlights() {
        flights = FlightDataHandler.fetchAllFlights();
        if (flights.isEmpty()) {
            flights = generateRandomFlights();
            for (Flight flight : flights) {
                FlightDataHandler.saveFlightToDatabase(flight);
            }
        }
    }

    private List<Flight> generateRandomFlights() {
        List<Flight> generatedFlights = new ArrayList<>();
        String[] cities = {"Manila", "Cebu", "Davao", "Palawan", "Boracay", "Iloilo", "Clark", "Siargao"};
        String[] airlines = {"Philippine Airlines", "Cebu Pacific", "AirAsia", "PAL Express"};
        Random rand = new Random();

        LocalDate today = LocalDate.of(2024, 12, 6);
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate nextMonthStart = LocalDate.of(2025, 1, 1);

        for (int i = 0; i < 50; i++) {
            String origin = cities[rand.nextInt(cities.length)];
            String destination;
            do {
                destination = cities[rand.nextInt(cities.length)];
            } while (destination.equals(origin));

            LocalDateTime departureTime;
            if (i < 15) {
                LocalDate randomDate = today.plusDays(rand.nextInt((int) ChronoUnit.DAYS.between(today, endOfWeek) + 1));
                departureTime = randomDate.atStartOfDay().plusHours(rand.nextInt(24)).plusMinutes(rand.nextInt(60));
            } else if (i < 35) {
                int randomWeekOffset = rand.nextInt(4) + 1;
                LocalDate randomWeekStart = today.plusWeeks(randomWeekOffset).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate randomDate = randomWeekStart.plusDays(rand.nextInt(7));
                departureTime = randomDate.atStartOfDay().plusHours(rand.nextInt(24)).plusMinutes(rand.nextInt(60));
            } else {
                LocalDate randomDate = nextMonthStart.plusDays(rand.nextInt(nextMonthStart.lengthOfMonth()));
                departureTime = randomDate.atStartOfDay().plusHours(rand.nextInt(24)).plusMinutes(rand.nextInt(60));
            }

            LocalDateTime arrivalTime = departureTime.plusHours(rand.nextInt(6) + 1);

            String cabinType = rand.nextBoolean() ? "Economy" : "Business";
            double price = cabinType.equals("Economy")
                    ? rand.nextDouble(1000, 3000)
                    : rand.nextDouble(3000, 5000);

            Flight flight = new Flight(
                    airlines[rand.nextInt(airlines.length)] + " " + rand.nextInt(1000),
                    origin,
                    destination,
                    departureTime,
                    arrivalTime,
                    price,
                    cabinType,
                    60
            );
            generatedFlights.add(flight);
        }
        return generatedFlights;
    }

    private void createUI() {
        setLayout(new BorderLayout());
        JPanel searchFilterPanel = new JPanel(new GridBagLayout());
        searchFilterPanel.setBackground(Color.decode("#0F149a"));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel searchLabel = new JLabel("Search Flights:");
        searchLabel.setForeground(Color.WHITE);
        searchField = new JTextField(20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        searchFilterPanel.add(searchLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        searchFilterPanel.add(searchField, gbc);
        gbc.gridwidth = 1;

        JLabel fromLabel = new JLabel("From:");
        fromLabel.setForeground(Color.WHITE);
        fromField = new JTextField(10);
        gbc.gridx = 0;
        gbc.gridy = 1;
        searchFilterPanel.add(fromLabel, gbc);
        gbc.gridx = 1;
        searchFilterPanel.add(fromField, gbc);

        JLabel toLabel = new JLabel("To:");
        toLabel.setForeground(Color.WHITE);
        toField = new JTextField(10);
        gbc.gridx = 2;
        gbc.gridy = 1;
        searchFilterPanel.add(toLabel, gbc);
        gbc.gridx = 3;
        searchFilterPanel.add(toField, gbc);

        String[] dates = {"Any Date","Today","Next Week", "Next Month"};
        String[] prices = {"Any Price", "Low to High", "High to Low"};
        String[] cabins = {"Any Cabin", "Economy", "Business"};
        String[] hours = {"Any Time", "Morning", "Afternoon", "Evening"};

        dateFilterCombo = createComboBox(dates);
        priceFilterCombo = createComboBox(prices);
        cabinFilterCombo = createComboBox(cabins);
        hourFilterCombo = createComboBox(hours);

        addFilter("Date:", dateFilterCombo, searchFilterPanel, gbc, 2);
        addFilter("Price:", priceFilterCombo, searchFilterPanel, gbc, 3);
        addFilter("Cabin:", cabinFilterCombo, searchFilterPanel, gbc, 4);
        addFilter("Time:", hourFilterCombo, searchFilterPanel, gbc, 5);

        String[] columnNames = {"Flight", "From", "To", "Departure", "Arrival", "Price", "Cabin", "Available Seats"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel = new DefaultTableModel(columnNames, 0);
        flightTable = new JTable(tableModel);

        int topBottomPadding = 20;
        int leftRightPadding = 15;
        PaddingTableCellRenderer renderer = new PaddingTableCellRenderer(topBottomPadding, leftRightPadding);
        for (int i = 0; i < flightTable.getColumnCount(); i++) {
            flightTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        Font tableFont = new Font("Arial", Font.PLAIN, 12);
        flightTable.setFont(tableFont);
        flightTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));

        populateTable(flights);

        JScrollPane scrollPane = new JScrollPane(flightTable);

        applyFilterButton = createButton("Apply Filters", Color.decode("#fd9b4d"));
        applyFilterButton.setActionCommand("Apply Filters");
        applyFilterButton.addActionListener(this);

        bookButton = createButton("Book Selected Flight", Color.decode("#fd9b4d"));
        bookButton.setActionCommand("Book Selected Flight");
        bookButton.addActionListener(this);

        goBackButton = createButton("Go Back", Color.decode("#4CAF50"));
        goBackButton.setActionCommand("Go Back");
        goBackButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(applyFilterButton);
        buttonPanel.add(bookButton);
        buttonPanel.add(goBackButton);

        add(searchFilterPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public static class PaddingTableCellRenderer extends DefaultTableCellRenderer {
        private final int topBottomPadding;
        private final int leftRightPadding;

        public PaddingTableCellRenderer(int topBottomPadding, int leftRightPadding) {
            this.topBottomPadding = topBottomPadding;
            this.leftRightPadding = leftRightPadding;
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(BorderFactory.createEmptyBorder(topBottomPadding, leftRightPadding, topBottomPadding, leftRightPadding));
            return label;
        }
    }

    private void addFilter(String labelText, JComboBox<String> comboBox, JPanel panel, GridBagConstraints gbc, int gridy) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = gridy;
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(comboBox, gbc);
    }

    private JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setBackground(Color.WHITE);
        return comboBox;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setPreferredSize(new Dimension(200, 40));
        return button;
    }

    private void populateTable(List<Flight> flightList) {
        tableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        for (Flight flight : flightList) {
            Object[] rowData = {
                    flight.getFlightName(),
                    flight.getOrigin(),
                    flight.getDestination(),
                    flight.getDepartureTime().format(formatter),
                    flight.getArrivalTime().format(formatter),
                    String.format("₱%.2f", flight.getPrice()),
                    flight.getCabin(),
                    flight.getAvailableSeats()
            };
            tableModel.addRow(rowData);
        }
    }

    private void refreshTableData() {
        flights = FlightDataHandler.fetchAllFlights();
        populateTable(flights);
    }

    private void openSeatSelection() {
        int selectedRow = flightTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a flight first.");
            return;
        }

        Flight selectedFlight = flights.get(selectedRow);
        new SeatSelectionDialog(this, selectedFlight, userEmail);
        FlightDataHandler.updateSeatBooking(selectedFlight);
        refreshTableData();

        if (selectedFlight.getAvailableSeats() == 0) {
            try (Connection conn = DBConnector.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM flights WHERE flight_name = ?")) {
                pstmt.setString(1, selectedFlight.getFlightName());
                pstmt.executeUpdate();
            } catch (SQLException | ClassNotFoundException e) {
                System.err.println("Error deleting fully booked flight: " + e.getMessage());
            }
            generateNewFlights();
            refreshTableData();
        }
    }


    public static void generateNewFlights() {
        Random random = new Random();

        String[] flightNames = {"Philippine Airlines", "Cebu Pacific", "AirAsia", "PAL Express"};
        String[] cabinTypes = {"Economy", "Business"};
        String[] locations = {"Manila", "Cebu", "Davao", "Palawan", "Boracay", "Iloilo", "Clark", "Siargao"};

        String flightName = flightNames[random.nextInt(flightNames.length)];
        String origin = locations[random.nextInt(locations.length)];
        String destination;
        do {
            destination = locations[random.nextInt(locations.length)];
        } while (destination.equals(origin));

        LocalDateTime departureTime;
        int category = random.nextInt(3);

        if (category == 0) {
            departureTime = LocalDateTime.of(2024, 12, random.nextInt(3) + 6, random.nextInt(24), random.nextInt(60));
        } else if (category == 1) {
            departureTime = LocalDateTime.of(2024, 12, random.nextInt(23) + 9, random.nextInt(24), random.nextInt(60));
        } else {
            departureTime = LocalDateTime.of(2025, 1, random.nextInt(31) + 1, random.nextInt(24), random.nextInt(60));
        }

        LocalDateTime arrivalTime = departureTime.plusHours(random.nextInt(6) + 1);

        String cabin = cabinTypes[random.nextInt(cabinTypes.length)];
        double price = cabin.equals("Economy")
                ? random.nextDouble(1000, 3000)
                : random.nextDouble(3000, 5000);

        int totalSeats = 60;

        String insertSQL = """
    INSERT INTO flights (flight_name, origin, destination, departure_time, arrival_time,
                         price, cabin, total_seats, available_seats, booked_seats)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """;

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, flightName);
            pstmt.setString(2, origin);
            pstmt.setString(3, destination);
            pstmt.setTimestamp(4, Timestamp.valueOf(departureTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(arrivalTime));
            pstmt.setDouble(6, price);
            pstmt.setString(7, cabin);
            pstmt.setInt(8, totalSeats);
            pstmt.setInt(9, totalSeats);
            pstmt.setString(10, "");

            pstmt.executeUpdate();
            System.out.println("Random flight generated and saved successfully.");
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error generating and saving random flight: " + e.getMessage());
        }
    }

    private void applyFilters() {
        List<Flight> filteredFlights = new ArrayList<>(flights);
        String searchText = searchField.getText().trim().toLowerCase();
        if (!searchText.isEmpty()) {
            filteredFlights.removeIf(flight ->
                    !flight.getFlightName().toLowerCase().contains(searchText)
            );
        }

        String fromText = fromField.getText().trim().toLowerCase();
        if (!fromText.isEmpty()) {
            filteredFlights.removeIf(flight ->
                    !flight.getOrigin().toLowerCase().contains(fromText)
            );
        }

        String toText = toField.getText().trim().toLowerCase();
        if (!toText.isEmpty()) {
            filteredFlights.removeIf(flight ->
                    !flight.getDestination().toLowerCase().contains(toText)
            );
        }
        String selectedDate = (String) dateFilterCombo.getSelectedItem();
        if (!"Any Date".equals(selectedDate)) {
            LocalDateTime now = LocalDateTime.of(2024, 12, 6, 0, 0); // Fixed to December 6, 2024, for consistency
            filteredFlights.removeIf(flight -> {
                LocalDateTime departure = flight.getDepartureTime();
                if ("Today".equals(selectedDate)) {
                    LocalDate endOfWeek = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                    return departure.toLocalDate().isBefore(now.toLocalDate()) || departure.toLocalDate().isAfter(endOfWeek);
                } else if ("Next Week".equals(selectedDate)) {
                    LocalDate nextWeekStart = LocalDate.of(2024, 12, 9);
                    LocalDate nextWeekEnd = LocalDate.of(2024, 12, 31);
                    return departure.toLocalDate().isBefore(nextWeekStart) || departure.toLocalDate().isAfter(nextWeekEnd);
                } else if ("Next Month".equals(selectedDate)) {
                    LocalDate nextMonthStart = now.toLocalDate().withDayOfMonth(1).plusMonths(1);
                    LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
                    return departure.toLocalDate().isBefore(nextMonthStart) || departure.toLocalDate().isAfter(nextMonthEnd);
                }
                return false;
            });

            filteredFlights.sort(Comparator.comparing(Flight::getDepartureTime));
        }

        String selectedPrice = (String) priceFilterCombo.getSelectedItem();
        if ("Low to High".equals(selectedPrice)) {
            bubbleSort(filteredFlights, Comparator.comparingDouble(Flight::getPrice));
        } else if ("High to Low".equals(selectedPrice)) {
            selectionSort(filteredFlights, Comparator.comparingDouble(Flight::getPrice).reversed());
        }

        String selectedCabin = (String) cabinFilterCombo.getSelectedItem();
        if (!"Any Cabin".equals(selectedCabin)) {
            filteredFlights.removeIf(flight -> !flight.getCabin().equalsIgnoreCase(selectedCabin));
        }

        String selectedHour = (String) hourFilterCombo.getSelectedItem();
        if (!"Any Time".equals(selectedHour)) {
            filteredFlights.removeIf(flight -> {
                int hour = flight.getDepartureTime().getHour();
                if ("Morning".equals(selectedHour)) {
                    return hour < 6 || hour >= 12;
                } else if ("Afternoon".equals(selectedHour)) {
                    return hour < 12 || hour >= 18;
                } else if ("Evening".equals(selectedHour)) {
                    return hour < 18;
                }
                return false;
            });
        }

        populateTable(filteredFlights);
    }

    private void bubbleSort(List<Flight> list, Comparator<Flight> comparator) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (comparator.compare(list.get(j), list.get(j + 1)) > 0) {
                    Collections.swap(list, j, j + 1);
                }
            }
        }
    }

    private void selectionSort(List<Flight> list, Comparator<Flight> comparator) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (comparator.compare(list.get(j), list.get(minIndex)) < 0) {
                    minIndex = j;
                }
            }
            Collections.swap(list, i, minIndex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == applyFilterButton) {
            applyFilters();
        }
        if (e.getSource() == bookButton) {
            openSeatSelection();
        }
        if (e.getSource() == goBackButton) {
            this.dispose();
            new userDashboard(userEmail);
        }
    }
}