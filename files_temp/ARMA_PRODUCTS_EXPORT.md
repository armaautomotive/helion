ARMA Products Export

This file is a consolidated export of current knowledge about Arma Automotive Inc. products and product-related assets based on the working markdown notes in this workspace.

It is intended as a clean snapshot rather than a legal or marketing final.


Product Portfolio Overview

Current product and asset categories:

1. ARMA Portable CNC Tube Notcher
2. CNC Tube Bender Kit
3. ADS CAD/CAM Software
4. Kit-car design and builder ecosystem assets
5. 5-axis body mold router capability
6. Brand/marketing products such as possible model kits


ARMA Portable CNC Tube Notcher

Status

This is the primary commercial product and the main revenue focus.


Core Description

The ARMA Portable CNC Tube Notcher is a portable CNC tube notching machine designed to provide automated tube fabrication capability without requiring a large permanent machine installation.


Primary Use Case

The machine is intended for:

1. Round steel tube and pipe
2. Round aluminum tube and pipe
3. Welded fabrication workflows
4. Shops and builders that want more capability and speed than hand tools


Current Price Direction

1. Standard price: $12,000 USD
2. Launch price: $10,500 USD for the first 5 units


Physical / Operational Notes

Known physical notes:

1. Approximate footprint: about 3 feet long, 2 feet wide, 50 inches high
2. Portable with wheels/casters
3. Designed to fit into a shop or garage corner when not in use

Current standard configuration:

1. Separate tube support carriage

Future/possible configuration:

1. Fixed table / gantry-style support for more permanent installation


Capabilities

The machine can:

1. Cut straight lengths
2. Cut coped intersections at angles
3. Support more than two-tube intersection joints
4. Cut tabs and corresponding slots for fit-up
5. Cut connected pie-cut sections for routing in tight spaces
6. Engrave part labels, directions, bend locations, and bend orientations


Material / Geometry Boundaries

Supported material/profile direction:

1. Round steel tube and pipe
2. Round aluminum tube and pipe

Supported OD range:

1. 1 inch to 4 inches outer diameter

Recommended wall limits discussed so far:

1. Less than 0.25 inches for steel
2. Less than 0.3 inches for aluminum

Not intended for:

1. Solid rod
2. Square tube
3. Rectangular tube
4. Angle / L-section
5. Other non-round profiles


Machine Architecture

The machine is a 2.5-axis system.

The cutter remains perpendicular to the central axis of the tube while:

1. Moving along the tube length
2. Rotating around the tube surface

It is not a 4-axis articulated cutting machine.


Cutting Method

The machine uses a plasma torch.

Important notes:

1. Plasma is less accurate than laser
2. Plasma is more affordable, practical, and easier to use than laser in this context
3. A customer-supplied plasma torch is required
4. The torch must support remote trigger
5. Compressed air is required

Known air requirement:

1. 120 psi
2. 6 CFM


Quality / Fit-Up Notes

Current quality direction:

1. Typical plasma kerf is about 0.9 mm
2. Software compensation is used
3. Joint fit-up is intended for welded fabrication
4. Many test joints fit without visible light pass-through, but this is not guaranteed in every case
5. Results depend on setup, material, torch behavior, and operation


Tube Length Handling

Important differentiator:

1. The separate support carriage allows effectively unlimited tube length in practice
2. Standard long stock such as 20-24 foot tube can be accommodated


Workflow

Typical user workflow:

1. Use a computer and ADS CAD/CAM software to import or design the part
2. Generate machine program / G-code
3. Transfer program by SD card
4. Load the machine
5. Feed and clamp the tube
6. Turn on torch and compressed air
7. Operate in a properly ventilated space

Straight cuts may require less design workflow, but most advanced cuts depend on software use.


Safety / Operating Conditions

The machine is intended for:

1. Indoor use only
2. Dry conditions only
3. Proper ventilation during plasma cutting
4. Eye protection during cutting
5. Flat and even ground for correct carriage movement

It should not be:

1. Operated outdoors
2. Exposed to rain or water


Electrical Notes

Known nameplate direction discussed:

1. 120 VAC
2. 60 Hz
3. 500 W
4. Single phase


Calibration and Assembly

Current sales/operating assumptions:

1. Machine ships on a pallet in multiple pieces
2. Customer assembly is required unless otherwise stated
3. Machine is factory-calibrated before delivery
4. Customer runs calibration after installation
5. Plasma torch tip must be calibrated after installation using machine controls


Support Model

Current support direction:

1. Basic remote onboarding and support included
2. Support limited to assembly, setup, calibration, normal operation, and basic troubleshooting
3. On-site service, custom CAD/CAM help, and process consulting are separate


Wear Items

Known wear-item direction:

1. Feed rollers include rubber grip material that wears over time
2. This is a normal wear item, not a warranty item
3. Replacement is customer maintenance unless otherwise agreed
4. Replacement requires opening the machine and using tools


Differentiators

Strongest differentiators identified so far:

1. Portable CNC format
2. Small footprint
3. Long-stock handling through separate carriage
4. Tabs and slots for fit-up
5. Engraving capability
6. Connected pie cuts
7. Bend-aware software integration
8. Much faster than hand methods such as hole saws and angle grinders


Key Constraints

Main constraints to communicate early:

1. Plasma-based, not laser
2. Round tube/pipe only
3. 2.5-axis, not 4-axis
4. Requires customer-supplied torch
5. Requires compressed air
6. Requires software workflow for most cuts
7. Requires ventilation
8. Extreme heavy-duty use can be thermally limiting


Commercial / Contract Notes

Current commercial direction:

1. Shipping quoted separately by region
2. International buyers handle import-side charges unless expressly agreed otherwise
3. Customer responsibilities and operating limits must be acknowledged explicitly
4. Sales agreement should tightly define support, wear items, and limitations


CNC Tube Bender Kit

Status

This is a real working product and a secondary commercialization priority.


Current Role

The bender kit is not the main focus right now.

It should be advanced when:

1. Notcher work is blocked by outside dependencies
2. The co-op has real downtime
3. The work can stay within a bounded productization scope


Current Positioning

1. Substantial R&D already invested
2. About three years of development
3. Patent application in progress
4. Strong technical product
5. Market believed to be smaller or less immediate than the notcher


Commercialization Scope

Current productization tasks identified:

1. BOM cleanup
2. Assembly documentation
3. Manual draft
4. Packaging plan
5. Labels/stickers
6. Compatibility definition
7. Support assumptions
8. Identification of unresolved commercial gaps


Strategic Role

The bender kit exists primarily as:

1. A secondary revenue product
2. A way to monetize sunk R&D
3. A companion product to the notcher and ADS ecosystem


ADS CAD/CAM Software

Status

ADS is real software with years of development behind it and is currently positioned as a support product for the CNC tools.


Core Role

ADS is primarily useful for:

1. Designing tube-frame structures
2. Generating CAM output for cutting and bending tube
3. Strengthening the notcher and bender ecosystem


Current Positioning

Current view:

1. ADS likely does not compete strongly enough as a standalone general CAD/CAM product against Fusion 360
2. ADS should remain a support product rather than a main business line
3. It creates differentiation for the hardware tools


Software Licensing Direction

Known software-commercial terms:

1. Licensed, not sold
2. Tied to user email address
3. Can be moved to another computer by the user
4. One year of updates included
5. Current version continues functioning indefinitely after the update period ends
6. Software license does not transfer automatically if the machine is sold
7. Software support is separate


Kit-Car Design / Builder Ecosystem

Status

The car remains a major strategic asset, but not the main current business line.


Known Vehicle/Product Notes

1. Mid-engine two-seat sports car
2. Steel tube frame chassis
3. Composite body
4. Custom glass windshield
5. Strong exterior design refined with Sabino Design


Builder-Ecosystem Direction

Current likely path:

1. Offer plans or builder package in some form
2. Offer body molds and related support
3. Possibly use vehicle-builder licensing instead of direct low-volume manufacturing
4. Use the car as a halo product for the tools


5-Axis Router / Body Mold Capability

Status

The company has a 5-axis router built to cut foam body molds.


Current Role

1. Internal production tool for body molds
2. Not currently viewed as a strong external product/business line
3. Possibly useful for future process experiments or internal manufacturing support


Body Panel Automation Ideas

Current conceptual direction:

1. Hand layup remains the likely immediate path
2. There is interest in automated deposition approaches for mold-based panel manufacturing
3. A plausible middle-ground concept is automated chopped-fiber or tow deposition with later vacuum bagging/infusion
4. This remains exploratory and is not an active commercial product


Brand / Model Kit Product

Status

Possible low-priority salable brand product.


Current concept:

1. 3D-printed model kits of the car
2. Not intended as a high-margin business line
3. Intended for marketing, brand awareness, gifting, and light revenue
4. Unfinished kits with bridges that the customer breaks apart, glues, and paints
5. Should be dropped once higher-value tool products are established


Roadmap Direction

Current likely product roadmap after the current notcher and bender:

1. Finish and validate the portable notcher
2. Productize and sell the bender kit
3. Consider notcher accessories and variants
4. Consider a higher-end notcher v2

Possible v2 directions discussed:

1. Non-portable architecture
2. Support for square and rectangular tube
3. Support for angle/L-section
4. Additional axis capability
5. More industrial buyer positioning


Product Portfolio Conclusion

Current strongest portfolio structure:

1. Portable CNC tube notcher as lead product
2. CNC tube bender kit as secondary companion product
3. ADS as support software
4. Car design, molds, and builder ecosystem as strategic adjacent assets
5. Brand products only if lightweight and non-distracting
