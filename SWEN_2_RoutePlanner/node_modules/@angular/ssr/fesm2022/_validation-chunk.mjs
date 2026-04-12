const HOST_HEADERS_TO_VALIDATE = new Set(['host', 'x-forwarded-host']);
const VALID_PORT_REGEX = /^\d+$/;
const VALID_PROTO_REGEX = /^https?$/i;
const VALID_HOST_REGEX = /^[a-z0-9_.-]+(:[0-9]+)?$/i;
const INVALID_PREFIX_REGEX = /^(?:\\|\/[/\\])|(?:^|[/\\])\.\.?(?:[/\\]|$)/;
function getFirstHeaderValue(value) {
  return value?.toString().split(',', 1)[0]?.trim();
}
function validateRequest(request, allowedHosts, disableHostCheck) {
  validateHeaders(request);
  if (!disableHostCheck) {
    validateUrl(new URL(request.url), allowedHosts);
  }
}
function validateUrl(url, allowedHosts) {
  const {
    hostname
  } = url;
  if (!isHostAllowed(hostname, allowedHosts)) {
    throw new Error(`URL with hostname "${hostname}" is not allowed.`);
  }
}
function cloneRequestAndPatchHeaders(request, allowedHosts) {
  let onError;
  const onErrorPromise = new Promise(resolve => {
    onError = resolve;
  });
  const clonedReq = new Request(request.clone(), {
    signal: request.signal
  });
  const headers = clonedReq.headers;
  const originalGet = headers.get;
  headers.get = function (name) {
    const value = originalGet.call(headers, name);
    if (!value) {
      return value;
    }
    validateHeader(name, value, allowedHosts, onError);
    return value;
  };
  const originalValues = headers.values;
  headers.values = function () {
    for (const name of HOST_HEADERS_TO_VALIDATE) {
      validateHeader(name, originalGet.call(headers, name), allowedHosts, onError);
    }
    return originalValues.call(headers);
  };
  const originalEntries = headers.entries;
  headers.entries = function () {
    const iterator = originalEntries.call(headers);
    return {
      next() {
        const result = iterator.next();
        if (!result.done) {
          const [key, value] = result.value;
          validateHeader(key, value, allowedHosts, onError);
        }
        return result;
      },
      [Symbol.iterator]() {
        return this;
      }
    };
  };
  const originalForEach = headers.forEach;
  headers.forEach = function (callback, thisArg) {
    originalForEach.call(headers, (value, key, parent) => {
      validateHeader(key, value, allowedHosts, onError);
      callback.call(thisArg, value, key, parent);
    }, thisArg);
  };
  headers[Symbol.iterator] = headers.entries;
  return {
    request: clonedReq,
    onError: onErrorPromise
  };
}
function validateHeader(name, value, allowedHosts, onError) {
  if (!value) {
    return;
  }
  if (!HOST_HEADERS_TO_VALIDATE.has(name.toLowerCase())) {
    return;
  }
  try {
    verifyHostAllowed(name, value, allowedHosts);
  } catch (error) {
    onError(error);
    throw error;
  }
}
function verifyHostAllowed(headerName, headerValue, allowedHosts) {
  const value = getFirstHeaderValue(headerValue);
  if (!value) {
    return;
  }
  const url = `http://${value}`;
  if (!URL.canParse(url)) {
    throw new Error(`Header "${headerName}" contains an invalid value and cannot be parsed.`);
  }
  const {
    hostname
  } = new URL(url);
  if (!isHostAllowed(hostname, allowedHosts)) {
    throw new Error(`Header "${headerName}" with value "${value}" is not allowed.`);
  }
}
function isHostAllowed(hostname, allowedHosts) {
  if (allowedHosts.has('*') || allowedHosts.has(hostname)) {
    return true;
  }
  for (const allowedHost of allowedHosts) {
    if (!allowedHost.startsWith('*.')) {
      continue;
    }
    const domain = allowedHost.slice(1);
    if (hostname.endsWith(domain)) {
      return true;
    }
  }
  return false;
}
function validateHeaders(request) {
  const headers = request.headers;
  for (const headerName of HOST_HEADERS_TO_VALIDATE) {
    const headerValue = getFirstHeaderValue(headers.get(headerName));
    if (headerValue && !VALID_HOST_REGEX.test(headerValue)) {
      throw new Error(`Header "${headerName}" contains characters that are not allowed.`);
    }
  }
  const xForwardedPort = getFirstHeaderValue(headers.get('x-forwarded-port'));
  if (xForwardedPort && !VALID_PORT_REGEX.test(xForwardedPort)) {
    throw new Error('Header "x-forwarded-port" must be a numeric value.');
  }
  const xForwardedProto = getFirstHeaderValue(headers.get('x-forwarded-proto'));
  if (xForwardedProto && !VALID_PROTO_REGEX.test(xForwardedProto)) {
    throw new Error('Header "x-forwarded-proto" must be either "http" or "https".');
  }
  const xForwardedPrefix = getFirstHeaderValue(headers.get('x-forwarded-prefix'));
  if (xForwardedPrefix && INVALID_PREFIX_REGEX.test(xForwardedPrefix)) {
    throw new Error('Header "x-forwarded-prefix" must not start with "\\" or multiple "/" or contain ".", ".." path segments.');
  }
}

export { cloneRequestAndPatchHeaders, getFirstHeaderValue, validateRequest, validateUrl };
//# sourceMappingURL=_validation-chunk.mjs.map
