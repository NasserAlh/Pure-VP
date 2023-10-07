# POC from Docker and VSCode Bookmap Add-on

The add-on "POC from Docker and VSCode" is a custom trading module developed for the Bookmap trading platform. This module is coded in Java and designed to run within a Docker container, allowing for ease of development and deployment via Visual Studio Code. The main goal of this add-on is to track and visualize price, volume, and Point of Control (POC) data in real-time, providing traders with valuable insights to make informed decisions.

## Features:

1. **Real-Time Indicators**:
   - Tracks and visualizes real-time price, volume, and Point of Control (POC) data.
   - Displays these indicators on primary and bottom graph types within the Bookmap interface.

2. **Volume Profile Analysis**:
   - Maintains a volume profile to analyze the distribution of trading volume at different price levels over time.
   - Identifies the Point of Control (POC), the price level with the highest traded volume.

3. **Trade Signal Generation**:
   - Generates trade signals based on the movement of price relative to the Point of Control (POC).
   - Signals are visually represented by translucent circles on the chart.

4. **Customizable Settings**:
   - Provides a settings panel for users to customize the tick distance from POC, aiding in tailoring the add-on to individual trading strategies.

5. **Timestamp Management**:
   - Handles timestamps to ensure accurate time-based data analysis.
   - Converts timestamps to New York time to align with the standard trading hours.

## Usage:

The `PocTrading` class is the main class implementing the `CustomModule`, `TradeDataListener`, `TimeListener`, and `CustomSettingsPanelProvider` interfaces provided by the Bookmap API. These interfaces allow the add-on to interact with live trading data, react to time updates, and provide a custom settings panel for user configuration.

### Initialization:

The `initialize` method is called upon loading the add-on, setting up the necessary indicators and their respective colors.

### Trade Data Handling:

The `onTrade` method is triggered on receiving new trade data. It updates the indicators, maintains the volume profile, calculates the Point of Control (POC), and checks the conditions for generating trade signals based on price movements relative to the POC.

### Trade Signal Visualization:

Translucent circles representing trade signals are created and added to the chart through the `createTranslucentCircle` method.

### Custom Settings Panel:

The `getCustomSettingsPanels` method provides a settings panel allowing users to customize the tick distance from POC via a slider.

### Time Handling:

The `onTimestamp` method updates the current timestamp, crucial for time-based analysis and trade signal generation.

## Settings Panel:

A settings panel named "Settings Ticks distance from POC" is provided for users to adjust the `POC_BUFFER` value which determines the tick distance from POC for trade signal generation. This value can be adjusted via a slider, providing a simple and intuitive user interface for customization.

---

The code structure is well-organized and straightforward, making it easy to understand and extend for further customization or additional functionality. By encapsulating the trading logic within a Docker container and allowing for development via Visual Studio Code, this add-on exemplifies a modern approach to creating custom trading modules for the Bookmap platform.