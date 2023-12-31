import { ColorModeScript } from '@chakra-ui/react';
import React, { StrictMode } from 'react';
import * as ReactDOM from 'react-dom/client';
import App from './App';
import reportWebVitals from './reportWebVitals';
import * as serviceWorker from './serviceWorker';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { APIContextProvider } from './components/APIContext';
import { MapProvider } from './components/MapContext';

const container = document.getElementById('root');
const root = ReactDOM.createRoot(container);

root.render(
  <GoogleOAuthProvider clientId={process.env.REACT_APP_clientId}>
    <StrictMode>
      <ColorModeScript />
      <MapProvider>
        <APIContextProvider>
          <App />
        </APIContextProvider>
      </MapProvider>
    </StrictMode>
  </GoogleOAuthProvider>
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://cra.link/PWA
serviceWorker.unregister();

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
