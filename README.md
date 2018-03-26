# currency-layer-api

A Java client API for http://currencylayer.com written in [kotlin](http://kotlinlang.org/)

## Getting Started

## Features

A simple API for querying currency rate related information from [currencylayer.com](currencylayer.com). It offers both blocking and asynchronous calls. The library provides types for the different responses.

## How to use

Include it as a dependency:

```
repositories {
    jcenter()
    maven { url "http://dl.bintray.com/helmethair-co/helmethair-co-oss" }
}

dependencies {
    compile 'co.helmethair:currency-layer-api:<latest-version>'
}
```

To use it in Java:
```Java
CurrencyLayerApi currencyLayerApi = new CurrencyLayerApi("<your-access-key>", true);
ListResponse listResponse = currencyLayerApi.list();
listResponse.getCurrencies();
```


To use it in kotlin:
```kotlin
val currencyLayerApi = CurrencyLayerApi("<your-access-key>", true)
val listResponse = currencyLayerApi.list()
listResponse.getCurrencies()
```
## Development

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You need to have a recent version of Java installed (8 and above). 

### Installing

```
git clone git@github.com:helmethair-co/currency-layer-api.git
```

## Running the tests

```
./gradlew test
```

## Built With

* [kotlin](http://kotlinlang.org/) - Statically typed programming language for modern multiplatform applications
* [gradle](https://maven.apache.org/) - Build tool
* [jackson](https://github.com/FasterXML/jackson-module-kotlin/) - Serialization / deserialization
* [fuel](https://github.com/kittinunf/Fuel) - Http networking

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning.

## Authors

* **Mark Vujevits** - *Initial work* - [helmethair.co](https://github.com/helmethair-co)

See also the list of [contributors](https://github.com/helmethair-co/currency-layer-api/contributors) who participated in this project.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details
