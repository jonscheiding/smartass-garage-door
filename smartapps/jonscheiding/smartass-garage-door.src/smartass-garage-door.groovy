/**
 *  Smartass Garage Door
 *
 *  Copyright 2016 Jon Scheiding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
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
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png")


preferences {
	section("Garage Door") {
		input "doorSwitch", "capability.momentary", title: "Opener", required: true
		input "doorContactSensor", "capability.contactSensor", title: "Open/Close Sensor", required: true
		input "doorAccelerationSensor", "capability.accelerationSensor",  title: "Movement Sensor", required: false
	}
	section("Car / Driver") {
		input "driver", "capability.presenceSensor", title: "Presence Sensor", required: true
	}
	section("Interior Door") {
		input "interiorDoor", "capability.contactSensor", title: "Open/Close Sensor", required: false
	}
	section("Notifications") {
		input "shouldSendPush", "enum", title: "Push Notifications", defaultValue: "All", options: ["None", "All", "Notices"]
	}
	section("Behavior") {
		input "openOnArrival", "bool", title: "Open On Arrival", defaultValue: true
        input "arrivalDebounceMinutes", "number", title: "... Except Minutes After Departure", defaultValue: 0
		input "closeOnDeparture", "bool", title: "Close On Departure", defaultValue: true
		input "closeOnEntry", "enum", title: "Close On Interior Door Entry", defaultValue: "Never", options: ["Never", "Open", "Closed"]
        input "closeOnEntryDelay", "number", title: "... After Minutes", defaultValue: 0
		input "closeOnModes", "mode", title: "Close When Entering Mode", multiple: true, required: false
	}
}

def onDriverArrived(evt) {
	state.lastArrival = now()

	if(openOnArrival) {
    	if(now() < state.lastDeparture + (arrivalDebounceMinutes * 60 * 1000)) {
        	notifyIfNecessary "${doorSwitch.displayName} will not be triggered because ${driver.displayName} left less than ${arrivalDebounceMinutes} minutes ago."
            return
        }
    
		pushDoorSwitch("open", "Opening ${doorSwitch.displayName} due to arrival of ${driver.displayName}.")
    }
}

def onDriverDeparted(evt) {
	state.lastDeparture = now()

	if(closeOnDeparture)
		pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to departure of ${driver.displayName}.")
}

def onInteriorDoorEntry(evt) {
	def expirationMinutes = 15

	if(state.lastArrival < state.lastClosed)
		return
	if(state.lastArrival < (now() - (expirationMinutes * 60 * 1000)))
		return

    if(closeOnEntryDelay <= 0) {
		pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to entry into ${interiorDoor.displayName}.")
    } else {
        state.lastEntry = now()
        runIn(closeOnEntryDelay * 60, onEntryDelayExpired)
    }
}

def onEntryDelayExpired() {
	if(state.lastEntry + closeOnEntryDelay * 60 > now()) 
    	return
    
    pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to entry into ${interiorDoor.displayName}.")
}

def onGarageDoorClosed(evt) {
	state.lastClosed = now()
}

def onModeChanged(evt) {
	if(!closeOnModes) return

	if(closeOnModes?.find { it == evt.value })
		pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} because mode changed to ${evt.value}.")
}

def pushDoorSwitch(desiredState, msg) {
	if(doorContactSensor.currentContact == desiredState) {
		notifyIfNecessary "${doorSwitch.displayName} will not be triggered because it is already ${desiredState}.", true
		return
	}
	if(doorAccelerationSensor && doorAccelerationSensor.currentAcceleration == "active") {
		notifyIfNecessary "${doorSwitch.displayName}  will not be triggered because it is currently in motion.", true
		return
	}

	notifyIfNecessary msg, false
	doorSwitch.push()
}

def notifyIfNecessary(msg, isNotice = false) {
	log.info msg
	log.debug("shouldSendPush=${shouldSendPush}, isNotice=${isNotice}")
	if(shouldSendPush == "0" || (shouldSendPush == "2" && !isNotice)) {
		return
	}

	sendPush msg
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
	subscribe(driver, "presence.present", onDriverArrived)
	subscribe(driver, "presence.not present", onDriverDeparted)

	subscribe(doorContactSensor, "contact.closed", onGarageDoorClosed)

	subscribe(location, "mode", onModeChanged)

	if(interiorDoor && closeOnEntry != "0") {
    	def contactEvent = (closeOnEntry == "1") ? "open" : "closed"
		subscribe(interiorDoor, "contact.${contactEvent}", onInteriorDoorEntry)
	}
}
