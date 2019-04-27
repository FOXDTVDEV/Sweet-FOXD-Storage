#!/bin/bash

mkdir $HOME/android/
cp -R app/build/outputs/apk/*.apk $HOME/android/

cd $HOME
git config --global user.email "hazae41@gmail.com"
git config --global user.name "Haz"

git clone --depth=10 --branch=dev  https://hazae41:$GITHUB_API_KEY@github.com/hazae41/sweet-ipfs  dev > /dev/null
cd dev
cp -Rf $HOME/android/* .

git add -A
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed"
git push -fq origin dev > /dev/null
echo "Published APK"