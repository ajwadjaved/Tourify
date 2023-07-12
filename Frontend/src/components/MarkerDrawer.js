import React, { useContext } from 'react';
import {
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  Button,
  useState,
  Input,
  Flex,
} from '@chakra-ui/react';
import { APIContext } from './APIContext';

//passing it marker state and method to change state so the X button can close the drawer
// also passing in marker object to render info in drawer
function MarkerDrawer({ isOpenFunc, isCloseFunc, markerObject }) {
  const { apiAttractions } = useContext(APIContext);

  if (!markerObject) {
    return null; // Return null when markerObject is null
  }

  return (
    <>
      <Drawer isOpen={isOpenFunc} placement="right" onClose={isCloseFunc}>
        <DrawerOverlay />

        <DrawerContent
          bg="white"
          border="5px solid orangered"
          borderRadius="20px"
          p="20px"
          w="80%"
        >
          <DrawerCloseButton />

          <DrawerHeader>{markerObject.name.name}</DrawerHeader>

          <DrawerBody>
            <br />
            Price: ${markerObject.price_dollars.price_dollars}
            <br />
            <img
              src={`/images/${markerObject.name.name.replaceAll(' ', '_')}.jpg`}
              alt={markerObject.name.name}
            />
          </DrawerBody>

          <DrawerFooter>
            {apiAttractions.map(attraction => {
              if (
                attraction.name === markerObject.name.name
              ) {
                return (
                  <div>
                    <p>Opening Hours:</p>
                    <p>
                      Monday: {attraction.openHour.mondayOpen} -{' '}
                      {attraction.openHour.mondayClose}
                    </p>
                    <p>
                      Tuesday: {attraction.openHour.tuesdayOpen} -{' '}
                      {attraction.openHour.tuesdayClose}
                    </p>
                    <p>
                      Wednsday: {attraction.openHour.wednesdayOpen} -{' '}
                      {attraction.openHour.wednesdayClose}
                    </p>
                    <p>
                      Thursday: {attraction.openHour.thursdayOpen} -{' '}
                      {attraction.openHour.thursdayClose}
                    </p>
                    <p>
                      Friday: {attraction.openHour.fridayOpen} -{' '}
                      {attraction.openHour.fridayClose}
                    </p>
                    <p>
                      Saturday: {attraction.openHour.saturdayOpen} -{' '}
                      {attraction.openHour.saturdayClose}
                    </p>
                    <p>
                      Sunday: {attraction.openHour.sundaydayOpen} -{' '}
                      {attraction.openHour.sundaydayClose}
                    </p>
                  </div>
                );
              }
              return null;
            })}
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  );
}

export default MarkerDrawer;
