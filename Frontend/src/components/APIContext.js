import React, { createContext, useState, useEffect, useContext } from 'react';
import { MapContext } from './MapContext';

const APIContext = createContext();

const APIContextProvider = ({ children }) => {
  const [apiAttractions, setAPIAttractions] = useState(null);
  const [apiCurrentWeather, setAPICurrentWeather] = useState(null);
  const [apiWeatherForecast, setAPIWeatherForecast] = useState(null);
  const [apiAllCurrentBusyness, setAPIAllCurrentBusyness] = useState(null);
  const [day1Params, setDay1Params] = useState(null);
  const [day2Params, setDay2Params] = useState(null);
  const [day3Params, setDay3Params] = useState(null);
  const [day4Params, setDay4Params] = useState(null);
  const [busynessPred, setBusynessPred] = useState(null);
  const [day2BusynessPred, setDay2BusynessPred] = useState(null);
  const [day3BusynessPred, setDay3BusynessPred] = useState(null);
  const [day4BusynessPred, setDay4BusynessPred] = useState(null);
  const [apiLoaded, setApiLoaded] = useState(false);
  const [globalUserInfo, setGlobalUserInfo] = useState();
  const [globalCredential, setGlobalCredential] = useState();
  const [checkinState, setCheckinState] = useState(false);
  const [badgeState, setBadgeState] = useState(null);
  const [newBadgeState, setNewBadgeState] = useState(null);
  const [currentModelTempParam, setCurrentModelTempParam] = useState(null);
  const [currentModelRainParam, setCurrentModelRainParam] = useState(null);
  const [updateClick, setUpdateClick] = useState(0);
  const [chartVisible, setChartVisible] = useState(false);
  const [chartData, setChartData] = useState(null);
  const [activeChart, setActiveChart] = useState(null); // for only showing the chart on the correct attraction

  const { mapCenter } = useContext(MapContext);

  useEffect(() => {
    const fetchAttractionData = async () => {
      try {
        const response = await fetch(
          // 'https://csi6220-2-vm1.ucd.ie/backend/api/attraction/getAllAttraction'
          'http://localhost:8001/api/attraction/getAllAttraction'
        );
        const data = await response.json(); //long/lat data
        console.log(data, 'THIS CAME FROM THE BACK END');
        const dataArray = data.data;
        console.log(dataArray, 'back end data without wrapper');

        setAPIAttractions(dataArray);
        setApiLoaded(true);

        console.log(apiLoaded, 'youve set it!!');
        console.log(apiLoaded, 'youve set it!!');
      } catch (error) {
        console.error('Error fetching attraction data:', error);
        setApiLoaded(false);
      }
    };

    const fetchWeatherData = async () => {
      try {
        const response = await fetch(
          `https://api.openweathermap.org/data/2.5/weather?lat=${mapCenter.lat}&lon=${mapCenter.lng}&units=imperial&appid=${process.env.REACT_APP_OPEN_WEATHER_API_KEY}`
        );
        const data = await response.json();
        console.log(data, 'THIS IS THE WEATHER');
        setAPICurrentWeather(data);
        setCurrentModelTempParam(Math.floor(data.main.temp)); // must convert kelvin to fahrenheit
        if (data.rain) {
          setCurrentModelRainParam(data.rain['1h'] / 25.4); // must convert millimetres to inches
        } else {
          setCurrentModelRainParam(0);
        }
      } catch (error) {
        console.error('Error fetching weather data:', error);
      }
    };

    fetchAttractionData();
    fetchWeatherData();
  }, []);

  useEffect(() => {
    const fetchAllCurrentBusynessData = async () => {
      try {
        if (currentModelTempParam && currentModelRainParam >= 0) {
          console.log(
            currentModelTempParam,
            currentModelRainParam,
            'these are the params for the model'
          );
          const response = await fetch(
            // `https://csi6220-2-vm1.ucd.ie/backend/api/attraction/getAllPrediction?temperature=${currentModelTempParam}&precipitation=${currentModelRainParam}`
            `http://localhost:8001/api/attraction/getAllPrediction?temperature=${currentModelTempParam}&precipitation=${currentModelRainParam}`
          );
          const data = await response.json();
          console.log(data, 'THIS IS THE MODEL PREDICTION');
          const dataArray = data.data;
          setAPIAllCurrentBusyness(dataArray);
        }
      } catch (error) {
        console.error('Error fetching model prediction data:', error);
      }
    };
    fetchAllCurrentBusynessData();
  }, [currentModelTempParam, currentModelRainParam, updateClick]);

  useEffect(() => {
    const getForecast = async () => {
      try {
        // setAttractionID(attractionID);
        const response = await fetch(
          `https://pro.openweathermap.org/data/2.5/forecast/hourly?lat=${mapCenter.lat}&lon=${mapCenter.lng}&units=imperial&appid=${process.env.REACT_APP_OPEN_WEATHER_API_KEY}`
        );
        const data = await response.json();
        console.log(data, 'THIS IS THE FORECAST');

        setAPIWeatherForecast(data.list);
      } catch (error) {
        console.error('Error fetching weather data:', error);
      }
    };
    getForecast();
  }, []);

  useEffect(() => {
    // processing the weather forecast into parameters for the model
    if (apiWeatherForecast) {
      // convert rain from millimetres to inches
      apiWeatherForecast.forEach(hour => {
        if (hour.rain) {
          const rainInches = hour.rain['1h'] / 25.4;
          hour.rain = rainInches;
        }
      });

      const tempArray = apiWeatherForecast.map(hour => {
        return {
          temperature: hour.main.temp,
        };
      });

      const rainArray = apiWeatherForecast.map(hour => {
        return {
          rain: hour.rain ? hour.rain : 0,
        };
      });

      const tempValues = tempArray.map(hour => hour.temperature);
      const rainValues = rainArray.map(hour => hour.rain);

      setDay1Params([
        {
          temperature: tempValues.slice(0, 24),
          rain: rainValues.slice(0, 24),
        },
      ]);

      setDay2Params([
        {
          temperature: tempValues.slice(24, 48),
          rain: rainValues.slice(24, 48),
        },
      ]);

      setDay3Params([
        {
          temperature: tempValues.slice(48, 72),
          rain: rainValues.slice(48, 72),
        },
      ]);

      setDay4Params([
        {
          temperature: tempValues.slice(72, 96),
          rain: rainValues.slice(72, 96),
        },
      ]);
    }
  }, [apiWeatherForecast]);

  const fetchBusynessPredictions = async (attractionID, params) => {
    if (params && attractionID) {
      if (chartData) {
        setChartData(null);
      }
      console.log(params, 'THESE ARE THE MODEL PARAMS');
      setChartVisible(true);
      setActiveChart(attractionID);
      console.log(attractionID);
      console.log(params[0].temperature);
      console.log(params[0].rain);
      try {
        const response = await fetch(
          // `https://csi6220-2-vm1.ucd.ie/backend/api/attraction/getOnePrediction?attraction_id=${attractionID}&temperatures=${params[0].temperature}&precipitation=${params[0].rain}`
          `http://localhost:8001/api/attraction/getOnePrediction?attraction_id=${attractionID}&temperatures=${params[0].temperature}&precipitation=${params[0].rain}`
        );
        const data = await response.json();
        console.log(data, 'THIS IS THE FORECAST PREDICTIONS');
        setChartData({
          labels: data.data.attractionPredictionDetailVOList.map(
            hour => hour.hour
          ),
          datasets: [
            {
              label: 'Busyness Rate',
              data: data.data.attractionPredictionDetailVOList.map(
                hour => hour.businessRate
              ),
              backgroundColor: 'rgba(255, 165, 0, 0.2)',
              borderColor: 'rgba(255, 165, 0, 1)',
              borderWidth: 1,
            },
          ],
        });
        // setDay1BusynessPred(data1.data.attractionPredictionDetailVOList);
        setBusynessPred({
          id: attractionID,
          busynessPreds: data.data.attractionPredictionDetailVOList,
        });
      } catch (error) {
        console.error('Error fetching weather data:', error);
      }
    }
  };

  return (
    <APIContext.Provider
      value={{
        apiAttractions,
        setAPIAttractions,
        apiLoaded,
        globalUserInfo,
        setGlobalUserInfo,
        setGlobalCredential,
        globalCredential,
        setCheckinState,
        checkinState,
        badgeState,
        setBadgeState,
        newBadgeState,
        setNewBadgeState,
        apiCurrentWeather,
        apiAllCurrentBusyness,
        currentModelTempParam,
        fetchBusynessPredictions,
        busynessPred,
        day2BusynessPred,
        day3BusynessPred,
        day4BusynessPred,
        updateClick,
        setUpdateClick,
        day1Params,
        day2Params,
        day3Params,
        day4Params,
        chartVisible,
        chartData,
        setChartData,
        activeChart,
        setChartVisible,
      }}
    >
      {children}
    </APIContext.Provider>
  );
};

export { APIContext, APIContextProvider };
