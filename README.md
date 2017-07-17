# My Android Security Guide

## Core Concepts
While this guide will focus mostly on Android security, I personally believe it's important that someone understand some core security concepts and processes if they plan on being effective at security. Examples of this include understanding threat models, the "Laws of Security", and defense in depth.

### Ten Immutable Laws of Security
Microsoft published a compendium of 10 "Laws of Security" which hold true in every situation. I recommend that everyone at least understand them and keep them in mind while you're threat modeling or assessing vulnerabilities. The original list is [here](https://technet.microsoft.com/en-us/library/hh278941.aspx).

1. If a bad guy can persuade you to run his program on your computer, it's not solely your computer anymore.
2. If a bad guy can alter the operating system on your computer, it's not your computer anymore.
3. If a bad guy has unrestricted physical access to your computer, it's not your computer anymore.
4. If you allow a bad guy to run active content in your website, it's not your website any more.
5. Weak passwords trump strong security.
6. A computer is only as secure as the administrator is trustworthy.
7. Encrypted data is only as secure as its decryption key.
8. An out-of-date antimalware scanner is only marginally better than no scanner at all.
9. Absolute anonymity isn't practically achievable, online or offline.
10. Technology is not a panacea.

### Threat Modeling
Threat modeling is a process in which threats are identified and enumerated. Afterwards, risk can be assessed and mitigations can be put in place. This is a process that should be done as early as possible, ideally before code is written. Security flaws can arise simply because the application's architecture enables it. 

Firstly, you must identify your security objectives. A naive, initial goal might be to make the security objective for your threat model "to build a 100% secure application" when in reality, an objective like this is both unattainable and directionless. Security is a state which is subjective and up to the interpretation of the beholder. For example, to most individuals, Facebook Messenger over HTTPS might be perfectly secure for them, however if you have concerns of CA compromise, nation-state threat actors, or social engineering attacks that compromise the other party then this communication channel would be seen as insecure or inadequate.

Secondly, you need to perform an application overview. In this step, you enumerate your application's data flows, components, and trust boundaries. You should be indiscriminate in the features you enumerate. In the next step we will determine which entities will require a closer look.

The third step is to identify which portions of your application need to be examined for threats. In this step you should look at obvious components such as authentication, file storage, or any components that interface with things outside of your application, such as other applications or users on the internet.

Lastly, start enumerating threats for each data flow and component. Using a classification scheme can help in coming up with ideas. The threat modeling classification scheme I, personally, find easiest to use is S.T.R.I.D.E.

* **S**poofing
* **T**ampering
* **R**epudiation
* **I**nformation Disclosure
* **D**enial of Service
* **E**levation of Privilege

When going over each data flow, look at each classification type and try putting your mind into an attacker's mindset; think to yourself, "What can I spoof/tamper-with here and what could it do?". Do this for all of the classifications and list all of the potential threats. It is usually helpful to have someone who has no idea how this software works involved in the process because they might ask questions that test your previous assumptions or come up with ideas that you might not have on your own.

After enumerating threats, come up with mitigations that you can employ in your applications. In some cases, you might determine that the original architecture for your application is flawed and will require a complete rearchitecting. In this case, make sure to restart threat modeling after the new design is created using the previous threats as a starting point when you STRIDE. It is generally good practice to validate your threat model is still relevant after implementation to verify that the design and threats are still true and relevant.

### Defense in Depth

Defense in Depth is the idea that a solid defense requires many layers. The common analogy is a castle.

![Castle](http://blog.trendmicro.com/trendlabs-security-intelligence/files/2013/02/understanding_apt2.png "Defense in Depth castle as viewed from sky")

While a castle provides strong defense on the outside, it is not impervious. If this castle only has a single layer then a lack of maintenance on the outside wall might be enough for an invading clan to get inside (a breach!). 

Defense in Depth suggests that we have many walls and protections that all work together. If one falls then another can take its place. For example, while a username and password combination is one layer of defense to protecting a user's account, two factor authentication is a second layer of defense that protects the user if the first layer falls. 

This should be enough of an introduction to some core security concepts for the rest to make sense. Each of the following sections will have an application associated with it that demonstrates some of the information listed in it. While the applications will generally contain multiple types of security protections (ie. input validation + cryptography), they individually will attempt to demonstrate a correct implementation (given the application) of each type of security measure. 

## Authentication

## User Profiles

## Packaging Apps

## Storing Data

## Permissions

## Networking

## Input Validation and other oddities

## WebViews

## Cryptography
Cryptography, or the art of writing secrets, allows us to manipulate a message in such a way as to include additional properties such as confidentiality, integrity, or authenticity. Normally, we don't write our own cryptographic algorithms but use predefined, correctly implemented implementations of these cryptographic algorithms. Such examples include AES (Advanced Encryption Standard), SHA (Secure Hashing Algorithm), or RSA.

In the Java ecosystem, cryptography is normally supplied via `Providers`. Each provider has a set of cryptographic algorithms they supply. Providers can be added, such as BouncyCastle, and can allow the developer to include lesser used or stronger cryptographic functions. The Android ecosystem works in the same way. 

### Key Generation
Before we encrypt or decrypt data, we need to create our encryption keys. The size for our keys depends on which encryption algorithm we plan on using and key sizes that we require for that algorithm. 

* AES 256-bit key
    ```java
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    generator.init(256);
    SecretKey encryptionKey = generator.generateKey();
    ```
* AES 128-bit key derived from a password
    ```java
    //user's password
    String password = "hunter2"; 
 
    //The salt should be securely randomly generated
    byte[] salt = new byte[32];
    (new SecureRandom()).nextBytes(salt);

    //4096 iterations of PBKDF2WithHmacSHA1 deriving 128 bits
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 4096, 128);
    SecretKey encryptionKey = factory.generateSecret(keySpec);
    ```
* RSA 2048-bit key pair
    ```java
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    ```

### Key Generation with AndroidKeyStore
The Android KeyStore is a module that allows you to store cryptographic primitives in a Hardware Security Module (HSM). This allows us to keep our encryption keys in a secure storage that only your app can access. Each key is given an `alias` which allows you to retrieve the key again as if it were a key-value data store. This alias need not be secret as the HSM separates each app/user into its own namespace; an alias for one app will refer to a different key with the same alias on another application. 

* AES 256-bit key intended for AES/GCM/NoPadding stored in Android KeyStore Provider
    ```java
    KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(), new SecureRandom());
    generator.generateKey();
    ```

The code for other types of keys or key pairs is similar. There also exist a number of user authentication settings you can specify for your keys that will force Android to prompt the user for device authentication whenever the key is accessed. This can be in the form of fingerprint authentication, pin, pattern or other things. 

### Encryption

## IPC

## Security with HTTPS and SSL

## SafetyNet and Attestination