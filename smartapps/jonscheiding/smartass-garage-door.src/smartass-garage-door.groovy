/**
 *  Smartass Garage Door
 *
 *  Copyright 2016 Jon Scheiding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smartass Garage Door",
    namespace: "jonscheiding",
    author: "Jon Scheiding",
    description: "Garage door app with the smarts to get by in this world.",
    category: "",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@tx.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@tx.png")


preferences {
	section("Garage Door") {
		input "doorSwitch", "capability.momentary", title: "Opener", required: true
	}
    section("Car / Driver") {
    	input "driver", "capability.presenceSensor", title: "Presence Sensor", required: true
	}
}

def driverPresence(evt) {
	if(evt.value == "present") 
    	driverArrived(evt)
    else
    	driverDeparted(evt)
}

def driverArrived(evt) {
	log.info "Door to be opened due to arrival of ${driver.displayName}."
	openDoor()
}

def driverDeparted(evt) {
	log.info "Door to be closed due to departure of ${driver.displayName}."
    closeDoor()
}

def openDoor() {
	doorSwitch.push()
}

def closeDoor() {
	doorSwitch.push()
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(driver, "presence", driverPresence)
}

// TODO: implement event handlers