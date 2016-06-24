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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


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
}

def driverArrived(evt) {
	log.info "Door to be opened due to arrival of ${driver.displayName}."
	openDoor()
}

def openDoor() {
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