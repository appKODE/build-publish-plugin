[← Documentation](../README.md)

# Troubleshooting

### Common Issues

#### Firebase Authentication Errors
```
> Failed to authenticate with Firebase
```
**Solution:**
1. Verify your `google-services.json` is in the correct location
2. Ensure the service account has the necessary permissions in Firebase Console
3. Check that the `appId` in your configuration matches your Firebase project

#### Play Store Upload Failures
```
> Failed to upload to Play Store: 403 Forbidden
```
**Solution:**
1. Verify your service account has the correct permissions in Google Play Console
2. Check that the package name in your app matches the one in Play Console
3. Ensure the service account is added to your app in Play Console

#### Changelog Generation Issues
```
> No Git tags found for changelog generation
```
**Solution:**
1. Make sure you have at least one Git tag
2. Verify your tag format matches the pattern in `buildTagPattern`
3. Run `git fetch --tags` to ensure all tags are available locally

