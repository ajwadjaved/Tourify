import React, { useState } from 'react';
import {
  Flex,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuDivider,
  Button,
  Image,
} from '@chakra-ui/react';
import { ChevronDownIcon } from '@chakra-ui/icons';
import attractions from '../static/attractions.json';

export default function DestinationInput({ map }) {
  const google = window.google;
  const [selectedAttraction, setSelectedAttraction] = useState(null);
  const [inputColour, setInputColour] = useState('#B5BBC6');

  const handleAttractionSelect = attraction => {
    setSelectedAttraction(attraction);
    setInputColour('black');
  };

  return (
    <Flex w={'270px'}>
      <Menu>
        <MenuButton
          as={Button}
          rightIcon={
            <ChevronDownIcon style={{ color: '#B5BBC6', fontSize: '30px' }} />
          }
          pt={'0.5px'}
          bg={'white'}
          w={'100%'}
          m={0}
          p={0}
          paddingLeft={'15px'}
          color={inputColour}
          fontFamily={'Roboto'}
          fontWeight={'Normal'}
          fontSize={'16px'}
          _hover={{ bg: 'white' }}
          _expanded={{ bg: 'white' }}
          style={{
            maxWidth: '100%',
            maxHeight: '100%',
            borderRadius: '20px',
            textAlign: 'left',
          }}
        >
          {!selectedAttraction ? 'I want to visit...' : selectedAttraction.name}
        </MenuButton>
        <MenuList
          mt={0}
          pt={0}
          boxShadow="2xl"
          maxHeight="200px"
          overflowY="auto"
        >
          {attractions.map(attraction => {
            return (
              <>
                <MenuItem
                  onClick={() => handleAttractionSelect(attraction)}
                  h={'32px'}
                  fontSize={'14px'}
                >
                  <Image
                    boxSize="1.5rem"
                    borderRadius="full"
                    src={attraction.image}
                    alt={attraction.name}
                    mr="12px"
                  />
                  <span>{attraction.name}</span>
                </MenuItem>
                <MenuDivider m={0} p={0} />
              </>
            );
          })}
        </MenuList>
      </Menu>
    </Flex>
  );
}