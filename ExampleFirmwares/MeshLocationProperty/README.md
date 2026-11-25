# Mesh Location and Property sample

A Bluetooth Mesh node for testing the Generic Location and Generic Property screens of the nRF Mesh
app. It is based on the nRF Connect SDK `light` sample and the SDK's tester application, and has been
built with nRF Connect SDK v3.0.2 for the nRF52840 DK (`nrf52840dk/nrf52840`).

## Models

| Element | Models |
|---|---|
| 1 | Configuration Server, Health Server, Generic Location Server, Generic Location Setup Server |
| 2 | Generic User, Admin, Manufacturer and Client Property Servers |

Values are kept in RAM and return to these defaults when the board is reset.

| State | Default |
|---|---|
| Global location | 59.9139, 10.7522, 23 m |
| Local location | north 1250 dm, east -340 dm, altitude 45 dm, ground floor (0), stationary, 1 s, 500 mm |
| Admin property UV Index `0x00B1` | 3, user access read / write |
| Admin property Presence Detected `0x004D` | 0, user access read |
| Manufacturer property Average Input Current `0x0002` | `10 27 00`, user access read |
| Manufacturer property Average Input Voltage `0x0003` | `20 4E 00`, user access prohibited |
| Client property | UV Index `0x00B1` |

## Flashing

A prebuilt image, `mesh_location_property_nrf52840dk.hex`, is included. Connect the board and run:

```bash
nrfutil device program --firmware mesh_location_property_nrf52840dk.hex --options chip_erase_mode=ERASE_ALL,reset=RESET_SYSTEM
```

## Building

```bash
nrfutil toolchain-manager launch --ncs-version v3.0.2 --chdir /opt/nordic/ncs/v3.0.2/nrf -- \
  west build -b nrf52840dk/nrf52840 <path to this folder> -d <build folder> --pristine
```

The image to flash is `<build folder>/merged.hex`.

## Testing with the app

1. Provision the node. Its name is "Mesh Loc Prop". While the app asks the node for attention, the LEDs blink.
2. Add an application key and bind it to the models on both elements.
3. Open Generic Location Server or Setup Server on element 1, and the property servers on element 2.
4. The node logs `Global location set`, `Local location set` and `Property 0x... set` on its UART when a set is received.

Hex values in the app are the raw property bytes, for example `07` for UV Index.
